package org.vitrivr.adampro.index.partition

import org.apache.spark.Partitioner
import org.apache.spark.sql.DataFrame
import org.vitrivr.adampro.datatypes.vector.Vector.MathVector
import org.vitrivr.adampro.entity.Entity.AttributeName
import org.vitrivr.adampro.entity.EntityNameHolder
import org.vitrivr.adampro.index.Index
import org.vitrivr.adampro.index.Index.IndexName
import org.vitrivr.adampro.main.{AdamContext, SparkStartup}
import org.vitrivr.adampro.utils.Logging

import scala.util.Random

/**
  * ADAMpar
  *
  * Silvan Heller
  * June 2016
  */
class RandomPartitioner(nPart: Int) extends Partitioner with Logging with Serializable {
  override def numPartitions: Int = nPart

  /**
    * Maps each key to a random partition ID, from 0 to `numPartitions - 1`.
    */
  override def getPartition(key: Any): Int = {
    (Random.nextFloat() * nPart).toInt
  }
}

object RandomPartitioner extends CustomPartitioner {
  override def partitionerName = PartitionerChoice.RANDOM

  /**
    * Throws each key in a random partition
    *
    * @param data        DataFrame you want to partition
    * @param attribute   Does not matter in this mode
    * @param indexName   will be used to store the partitioner in the Catalog
    * @param nPartitions how many partitions shall be created
    * @return the partitioned DataFrame
    */
  override def apply(data: DataFrame, attribute: Option[AttributeName], indexName: Option[IndexName], nPartitions: Int, options: Map[String, String] = Map[String, String]())(implicit ac: AdamContext): DataFrame = {
    import ac.spark.implicits._

    val schema = data.schema
    SparkStartup.catalogOperator.dropPartitioner(indexName.get)
    SparkStartup.catalogOperator.createPartitioner(indexName.get, nPartitions, null, RandomPartitioner)
    val toPartition = if (attribute.isDefined) data.map(r => (r.getAs[Any](attribute.get), r)) else data.map(r => (r.getAs[Any](Index.load(indexName.get).get.pk.name), r))
    ac.sqlContext.createDataFrame(toPartition.rdd.partitionBy(new RandomPartitioner(nPartitions)).mapPartitions(r => r.map(_._2), true), schema)
  }

  /** Returns the partitions to be queried for a given Feature vector
    * Returns Random Partitions
    * */
  override def getPartitions(q: MathVector, dropPercentage: Double, indexName: EntityNameHolder)(implicit ac: AdamContext): Seq[Int] = {
    val nPart = SparkStartup.catalogOperator.getNumberOfPartitions(indexName).get
    Random.shuffle(Seq.tabulate(nPart)(el => el)).drop((nPart * dropPercentage).toInt)
  }
}
