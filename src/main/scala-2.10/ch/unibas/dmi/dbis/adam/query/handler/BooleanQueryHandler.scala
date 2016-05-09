package ch.unibas.dmi.dbis.adam.query.handler

import java.util.concurrent.TimeUnit

import ch.unibas.dmi.dbis.adam.config.AdamConfig
import ch.unibas.dmi.dbis.adam.entity.Entity._
import ch.unibas.dmi.dbis.adam.entity.EntityHandler
import ch.unibas.dmi.dbis.adam.main.{AdamContext, SparkStartup}
import ch.unibas.dmi.dbis.adam.query.query.{BooleanQuery, TupleIDQuery}
import ch.unibas.dmi.dbis.adam.query.scanner.MetadataScanner
import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.apache.log4j.Logger
import org.apache.spark.sql.DataFrame

import scala.util.{Failure, Success, Try}

/**
 * adamtwo
 *
 * Ivan Giangreco
 * November 2015
 */
private[query] object BooleanQueryHandler {
  val log = Logger.getLogger(getClass.getName)
  /**
    * Performs a Boolean query on the metadata.
    *
    * @param entityname
    * @param query
    * @return
    */
  def getBQIds(entityname: EntityName, query: Option[BooleanQuery])(implicit ac: AdamContext): Option[DataFrame] = {
    log.debug("performing metadata-based boolean query on " + entityname)
    BooleanQueryLRUCache.get(entityname, query).get
  }

  /**
    * Performs a Boolean query on the metadata.
    *
    * @param entityname
    * @param query
    * @return
    */
  def getTIQIds(entityname: EntityName, query: Option[TupleIDQuery[_]])(implicit ac: AdamContext): Option[DataFrame] = {
    log.debug("performing id query on " + entityname)
    val entity = EntityHandler.load(entityname).get
    if(query.isDefined){
      import org.apache.spark.sql.functions.col

      val filter = query.get.tidFilter.toSeq
      Some(entity.getFeaturedata.select(entity.pk).filter(col(entity.pk).isin(filter : _*)))
    } else {
      None
    }
  }


  /**
    * Returns all metadata tuples from the given entity.
    *
    * @param entityname
    * @return
    */
  def getData(entityname: EntityName, query : Option[BooleanQuery] = None)(implicit ac: AdamContext) : Option[DataFrame] = {
    BooleanQueryLRUCache.get(entityname, query).get
  }


  /**
    * Performs a Boolean query on the metadata where the ID only is compared.
    *
    * @param entityname
    * @param filter tuple ids to filter on
    * @return
    */
  def getData(entityname: EntityName, filter : DataFrame)(implicit ac: AdamContext): Option[DataFrame] = {
    log.debug("retrieving metadata for " + entityname)
    MetadataScanner(EntityHandler.load(entityname).get, filter)
  }


  object BooleanQueryLRUCache {
    private val maximumCacheSize = AdamConfig.maximumCacheSizeBooleanQuery
    private val expireAfterAccess = AdamConfig.expireAfterAccessBooleanQuery

    private val bqCache = CacheBuilder.
      newBuilder().
      maximumSize(maximumCacheSize).
      expireAfterAccess(expireAfterAccess, TimeUnit.MINUTES).
      build(
        new CacheLoader[(EntityName, Option[BooleanQuery]), Option[DataFrame]]() {
          def load(query: (EntityName, Option[BooleanQuery])): Option[DataFrame] = {
            import SparkStartup.Implicits._
            val df = MetadataScanner.apply(EntityHandler.load(query._1).get, query._2)

            if(df.isDefined){
              df.get.cache()
            }

            return df
          }
        }
      )


    def get(entityname: EntityName, bq : Option[BooleanQuery]): Try[Option[DataFrame]] = {
      try {
        Success(bqCache.get((entityname, bq)))
      } catch {
        case e : Exception =>
          log.error(e.getMessage)
          Failure(e)
      }
    }
  }

}
