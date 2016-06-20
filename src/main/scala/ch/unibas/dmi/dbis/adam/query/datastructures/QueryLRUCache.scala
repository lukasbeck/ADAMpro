package ch.unibas.dmi.dbis.adam.query.datastructures

import java.util.concurrent.TimeUnit

import ch.unibas.dmi.dbis.adam.config.AdamConfig
import ch.unibas.dmi.dbis.adam.exception.QueryNotCachedException
import com.google.common.cache.{CacheBuilder, CacheLoader}
import ch.unibas.dmi.dbis.adam.utils.Logging
import org.apache.spark.sql.DataFrame

import scala.util.{Failure, Success, Try}

/**
  * adampro
  *
  * Ivan Giangreco
  * April 2016
  */
object QueryLRUCache extends Logging {
  private val maximumCacheSize = AdamConfig.maximumCacheSizeQueryResults
  private val expireAfterAccess = AdamConfig.expireAfterAccessQueryResults

  private val queryCache = CacheBuilder.
    newBuilder().
    maximumSize(maximumCacheSize).
    expireAfterAccess(expireAfterAccess, TimeUnit.MINUTES).
    build(
      new CacheLoader[String, DataFrame]() {
        def load(queryid: String): DataFrame = {
          log.trace("cache miss for query " + queryid)
          null
        }
      }
    )

  /**
    * Gets query from cache if it has been performed already.
    *
    * @param queryid
    */
  def get(queryid: String): Try[DataFrame] = {
    try {
      val result = queryCache.getIfPresent(queryid)
      if (result != null) {
        log.debug("getting query results from cache")
        Success(result)
      } else {
        Failure(QueryNotCachedException())
      }
    } catch {
      case e: Exception =>
        Failure(e)
    }
  }

  /**
    * Puts data into query cache
    *
    * @param queryid
    * @param data
    */
  def put(queryid : String, data : DataFrame): Unit ={
    log.debug("putting query results into cache")
    queryCache.put(queryid, data)
  }
}

case class QueryCacheOptions(useCached: Boolean = false, putInCache: Boolean = false)