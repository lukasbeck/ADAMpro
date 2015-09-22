package ch.unibas.dmi.dbis.adam.main

import ch.unibas.dmi.dbis.adam.storage.components.{IndexStorage, TableStorage}
import ch.unibas.dmi.dbis.adam.storage.engine.{OrcDataStorage, ParquetDataStorage}
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

/**
 * adamtwo
 *
 * Ivan Giangreco
 * August 2015
 */
object  SparkStartup {
  val sparkConfig = new SparkConf().setAppName("ADAMtwo").setMaster("local[128]")
  sparkConfig.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  sparkConfig.set("spark.kryoserializer.buffer.max", "512m");
  sparkConfig.set("spark.kryoserializer.buffer", "256");
  sparkConfig.set("spark.driver.maxResultSize", "0");
  sparkConfig.set("spark.driver.memory", "9g");
  sparkConfig.set("spark.rdd.compress", "true");
  sparkConfig.set("spark.parquet.block.size", (1024 * 1024 * 16).toString)
  sparkConfig.set("spark.sql.parquet.compression.codec", "lzo")
  sparkConfig.registerKryoClasses(Array()) //TODO: check this!

  val sc = new SparkContext(sparkConfig)
  //val sqlContext = new SQLContext(sc)
  val sqlContext = new HiveContext(sc)

  sqlContext.setConf("spark.sql.avro.compression.codec", "deflate")
  sqlContext.setConf("spark.sql.avro.deflate.level", "5")
  sqlContext.setConf("spark.parquet.block.size", (1024 * 1024 * 16).toString)

    val tableStorage: TableStorage = ParquetDataStorage
    val indexStorage: IndexStorage = ParquetDataStorage
}
