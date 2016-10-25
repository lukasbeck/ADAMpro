package ch.unibas.dmi.dbis.adam.api

import ch.unibas.dmi.dbis.adam.utils.Logging

import scala.util.{Failure, Try}

/**
  * adamtwo
  *
  * Ivan Giangreco
  * May 2016
  */
abstract class GenericOp extends Logging {
  def execute[T](desc: String)(op: => Try[T]): Try[T] = {
    try {
      log.debug("starting " + desc)
      val t1 = System.currentTimeMillis
      val res = op

      if(res.isFailure){
        throw res.failed.get
      }

      val t2 = System.currentTimeMillis
      log.debug("performed " + desc + " in " + (t2 - t1) + " msecs")
      res
    } catch {
      case e: Exception =>
        log.error("error in " + desc, e)
        Failure(e)
    }
  }
}
