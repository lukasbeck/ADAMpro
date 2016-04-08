package ch.unibas.dmi.dbis.adam.index.structures.pq

import ch.unibas.dmi.dbis.adam.entity.Entity
import ch.unibas.dmi.dbis.adam.entity.Entity.EntityName
import ch.unibas.dmi.dbis.adam.index.Index._
import ch.unibas.dmi.dbis.adam.index._
import ch.unibas.dmi.dbis.adam.index.structures.IndexTypes
import ch.unibas.dmi.dbis.adam.main.SparkStartup
import org.apache.log4j.Logger
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.rdd.RDD
import org.apache.spark.util.random.ADAMSamplingUtils

import scala.collection.immutable.IndexedSeq

/**
  * adampro
  *
  * Ivan Giangreco
  * April 2016
  */
class PQIndexer(nsq: Int, trainingSize: Int) extends IndexGenerator with Serializable {
  @transient lazy val log = Logger.getLogger(getClass.getName)

  override def indextypename : IndexTypeName = IndexTypes.PQINDEX

  override def index(indexname: IndexName, entityname: EntityName, data: RDD[IndexingTaskTuple]): Index = {
    val n = Entity.countTuples(entityname)
    val fraction = ADAMSamplingUtils.computeFractionForSampleSize(trainingSize, n, false)
    val trainData = data.sample(false, fraction)
    val collected = trainData.collect()
    val indexMetaData = train(collected)

    val d = collected.head.feature.size


    log.debug("PQ indexing...")

    val indexdata = data.map(
      datum => {
        val hash = datum.feature.toArray
          .grouped(d / nsq).toSeq
          .zipWithIndex
          .map{case(split,idx) => indexMetaData.models(idx).predict(Vectors.dense(split.map(_.toDouble))).toByte}
        ByteArrayIndexTuple(datum.id, hash)
      })

    import SparkStartup.sqlContext.implicits._
    new PQIndex(indexname, entityname, indexdata.toDF, indexMetaData)
  }

  /**
    *
    * @param trainData
    * @return
    */
  private def train(trainData: Array[IndexingTaskTuple]): PQIndexMetaData = {
    val numIterations = 100
    val nsqbits : Int = 8 //index produces a byte array index tuple
    val numClusters : Int = 2 ^ nsqbits

    val d = trainData.head.feature.size

    val rdds = trainData.map(_.feature).flatMap(t =>
      t.toArray.grouped(d / nsq).toSeq.zipWithIndex)
      .groupBy(_._2)
      .mapValues(vs => vs.map(_._1))
      .mapValues(vs => vs.map(v => Vectors.dense(v.map(_.toDouble))))
      .mapValues(vs => SparkStartup.sc.parallelize(vs))
      .toIndexedSeq
      .sortBy(_._1)
      .map(_._2)


    val clusters: IndexedSeq[KMeansModel] = rdds.map { rdd =>
      KMeans.train(rdd, numClusters, numIterations)
    }

    PQIndexMetaData(clusters, nsq)
  }
}

object PQIndexer {
  /**
    *
    * @param properties
    */
  def apply(properties : Map[String, String] = Map[String, String]()) : IndexGenerator = {
    val nsq = properties.getOrElse("nsq", "8").toInt
    val trainingSize = properties.getOrElse("trainingSize", "500").toInt

    new PQIndexer(nsq, trainingSize)
  }
}