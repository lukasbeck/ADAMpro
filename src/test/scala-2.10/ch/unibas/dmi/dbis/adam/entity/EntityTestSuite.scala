package ch.unibas.dmi.dbis.adam.entity

import java.sql.DriverManager

import ch.unibas.dmi.dbis.adam.config.{AdamConfig, FieldNames}
import ch.unibas.dmi.dbis.adam.datatypes.feature.{FeatureVectorWrapper, FeatureVectorWrapperUDT}
import ch.unibas.dmi.dbis.adam.entity.FieldTypes.FieldType
import ch.unibas.dmi.dbis.adam.main.SparkStartup
import org.apache.spark.sql.{types, Row}
import org.apache.spark.sql.types._
import org.scalatest.Matchers._
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{FeatureSpec, GivenWhenThen}

import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * adamtwo
  *
  * Ivan Giangreco
  * March 2016
  */
class EntityTestSuite extends FeatureSpec with GivenWhenThen with Eventually with IntegrationPatience {
  SparkStartup

  def fixture =
    new {
      val connection = {
        Class.forName("org.postgresql.Driver").newInstance
        DriverManager.getConnection(AdamConfig.jdbcUrl, AdamConfig.jdbcUser, AdamConfig.jdbcPassword)
      }
    }

  def getRandomName(len: Int = 10) = {
    val sb = new StringBuilder(len)
    val ab = "abcdefghijklmnopqrstuvwxyz"
    for (i <- 0 until len) {
      sb.append(ab(Random.nextInt(ab.length)))
    }
    sb.toString
  }

  feature("data definition") {
    scenario("create an entity") {
      Given("a database with a few elements already")
      val givenEntities = Entity.list()

      When("a new random entity (without any metadata) is created")
      val entityname = getRandomName()
      Entity.create(entityname)

      Then("the entity, and only the entity should exist")
      val finalEntities = Entity.list()
      eventually {
        finalEntities.size shouldBe givenEntities.size + 1
      }
      eventually {
        finalEntities.contains(entityname)
      }
    }

    scenario("drop an existing entity") {
      Given("there exists one entity")
      val entityname = getRandomName()
      Entity.create(entityname)
      assert(Entity.list().contains(entityname))

      When("the enetity is dropped")
      Entity.drop(entityname)

      Then("no entity should exist")
      assert(!Entity.list().contains(entityname))
    }

    scenario("create an entity with metadata") {
      Given("a database with a few elements already")
      val givenEntities = Entity.list()

      val fieldTemplate = Seq(
        ("stringfield", FieldTypes.STRINGTYPE, "text"),
        ("floatfield", FieldTypes.FLOATTYPE, "real"),
        ("doublefield", FieldTypes.DOUBLETYPE, "double precision"),
        ("intfield", FieldTypes.INTTYPE, "integer"),
        ("longfield", FieldTypes.LONGTYPE, "bigint"),
        ("booleanfield", FieldTypes.BOOLEANTYPE, "boolean")
      )

      When("a new random entity with metadata is created")
      val entityname = getRandomName()
      val fields: Map[String, FieldType] = fieldTemplate.map(ft => (ft._1, ft._2)).toMap
      Entity.create(entityname, Option(fields))

      Then("the entity, and only the entity should exist")
      val entities = Entity.list()
      val finalEntities = Entity.list()
      assert(finalEntities.size == givenEntities.size + 1)
      assert(finalEntities.contains(entityname))

      And("The metadata table should have been created")
      val connection = fixture.connection
      val result = connection.createStatement().executeQuery("SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '" + entityname + "'")

      val lb = new ListBuffer[(String, String)]()
      while (result.next) {
        lb.+=((result.getString(1), result.getString(2)))
      }

      val dbNames = lb.toList.toMap
      val templateNames = fieldTemplate.map(ft => (ft._1, ft._3)).toMap + ("adamtwoid" -> "integer")

      assert(dbNames.size == templateNames.size)

      val keys = dbNames.keySet

      And("The metadata table should contain the same columns (with the corresponding data type)")
      assert(keys.forall({ key => dbNames(key) == templateNames(key) }))
    }


    scenario("drop an entity with metadata") {
      Given("an entity with metadata")
      val entityname = getRandomName()
      val fields: Map[String, FieldType] = Map[String, FieldType](("stringfield" -> FieldTypes.STRINGTYPE))
      Entity.create(entityname, Option(fields))

      val connection = fixture.connection
      val preResult = connection.createStatement().executeQuery("SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + entityname + "'")
      var tableCount = 0
      while (preResult.next) {
        tableCount += 1
      }

      When("the entity is dropped")
      Entity.drop(entityname)

      Then("the metadata entity is dropped as well")
      val postResult = connection.createStatement().executeQuery("SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + entityname + "'")
      while (postResult.next) {
        tableCount -= 1
      }

      assert(tableCount == 0)
    }
  }

  case class WrappingTuple(featureVectorWrapper: FeatureVectorWrapper)


  feature("data manipulation") {

    scenario("insert data in an entity without metadata") {
      Given("an entity without metadata")
      val entityname = getRandomName()
      Entity.create(entityname)

      val ntuples = Random.nextInt(1000)
      val ndims = 100

      val schema = StructType(Seq(
        StructField(FieldNames.featureColumnName, new FeatureVectorWrapperUDT, false)
      ))

      val rdd = SparkStartup.sc.parallelize((0 until ntuples).map(id =>
        Row(new FeatureVectorWrapper(Seq.fill(ndims)(Random.nextFloat())))
      ))

      val data = SparkStartup.sqlContext.createDataFrame(rdd, schema)

      When("data without metadata is inserted")
      Entity.insertData(entityname, data)

      Then("the data is available without metadata")
      assert(Entity.countTuples(entityname) == ntuples)
    }

    scenario("insert data in an entity with metadata") {
      Given("an entity with metadata")
      val entityname = getRandomName()
      //every field has been created twice, one is not filled to check whether this works too
      val fieldTemplate = Seq(
        ("stringfield", FieldTypes.STRINGTYPE, "text"),
        ("stringfieldunfilled", FieldTypes.STRINGTYPE, "text"),
        ("floatfield", FieldTypes.FLOATTYPE, "real"),
        ("floatfieldunfilled", FieldTypes.FLOATTYPE, "real"),
        ("doublefield", FieldTypes.DOUBLETYPE, "double precision"),
        ("doublefieldunfilled", FieldTypes.DOUBLETYPE, "double precision"),
        ("intfield", FieldTypes.INTTYPE, "integer"),
        ("intfieldunfilled", FieldTypes.LONGTYPE, "bigint"),
        ("longfield", FieldTypes.LONGTYPE, "bigint"),
        ("longfieldunfilled", FieldTypes.LONGTYPE, "bigint"),
        ("booleanfield", FieldTypes.BOOLEANTYPE, "boolean"),
        ("booleanfieldunfilled", FieldTypes.BOOLEANTYPE, "boolean")
      )
      val fields: Map[String, FieldType] = fieldTemplate.map(ft => (ft._1, ft._2)).toMap
      Entity.create(entityname, Option(fields))

      val ntuples = Random.nextInt(1000)
      val ndims = 100
      val stringLength = 10
      val maxInt = 50000

      When("data is inserted")
      val schema = StructType(Seq(
        StructField(FieldNames.featureColumnName, new FeatureVectorWrapperUDT, false),
        StructField("stringfield", types.StringType, false),
        StructField("floatfield", types.FloatType, false),
        StructField("doublefield", types.DoubleType, false),
        StructField("intfield", types.IntegerType, false),
        StructField("longfield", types.LongType, false),
        StructField("booleanfield", types.BooleanType, false),
        StructField("unusedfield", types.IntegerType, false)
      ))


      val rdd = SparkStartup.sc.parallelize((0 until ntuples).map(id =>
        Row(new FeatureVectorWrapper(Seq.fill(ndims)(Random.nextFloat())),
          Random.nextString(stringLength),
          Random.nextFloat(),
          Random.nextDouble(),
          Random.nextInt(maxInt),
          Random.nextLong(),
          Random.nextBoolean(),
          Random.nextInt(10) + maxInt //unused field
          )))

      val data = SparkStartup.sqlContext.createDataFrame(rdd, schema)

      When("data with metadata is inserted")
      Entity.insertData(entityname, data)

      Then("the data is available with metadata")
      assert(Entity.countTuples(entityname) == ntuples)

      val connection = fixture.connection

      val countResult = connection.createStatement().executeQuery("SELECT COUNT(*) AS count FROM " + entityname)
      val tableCount = countResult.getInt("count")

      assert(tableCount == ntuples)

      //filled fields should be filled
      val randomRowResult = connection.createStatement().executeQuery("SELECT * FROM " + entityname + " ORDER BY RANDOM() LIMIT 1")
      assert(randomRowResult.getString("stringfield").length == stringLength)
      assert(randomRowResult.getFloat("floatField") >= 0)
      assert(randomRowResult.getDouble("doubleField") >= 0)
      assert(randomRowResult.getInt("intfield") < maxInt)
      assert(randomRowResult.getLong("longfield") > 0)
      assert((randomRowResult.getBoolean("booleanfield")) || (!randomRowResult.getBoolean("booleanfield")))

      //unfilled fields should be unfilled
      assert(randomRowResult.getString("stringfieldunfilled").length == 0)
      assert(randomRowResult.getFloat("floatFieldunfilled") < 10e-8)
      assert(randomRowResult.getDouble("doubleFieldunfilled") < 10e-8)
      assert(randomRowResult.getInt("intfieldunfilled") == 0)
      assert(randomRowResult.getLong("longfieldunfilled") == 0)
      assert(randomRowResult.getBoolean("booleanfieldunfilled") == false)
    }
  }
}