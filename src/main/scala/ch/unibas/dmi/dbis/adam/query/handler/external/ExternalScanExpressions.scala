package ch.unibas.dmi.dbis.adam.query.handler.external

import ch.unibas.dmi.dbis.adam.entity.Entity._
import ch.unibas.dmi.dbis.adam.main.AdamContext
import ch.unibas.dmi.dbis.adam.query.handler.generic.QueryExpression

/**
  * adampro
  *
  * Ivan Giangreco
  * May 2016
  */
object ExternalScanExpressions {
  def toQueryExpression(handlername: String, entityname: EntityName, params: Map[String, String], id: Option[String] = None)(implicit ac: AdamContext): QueryExpression = {
    handlername match {
      case "solr" => new SolrScanExpression(entityname, handlername, params, id)
      case "gis" => new GisScanExpression(entityname, handlername, params, id)
      case _ => null
    }
  }
}
