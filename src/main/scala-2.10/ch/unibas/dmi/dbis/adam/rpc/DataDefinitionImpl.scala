package ch.unibas.dmi.dbis.adam.rpc

import ch.unibas.dmi.dbis.adam.api.{CountOp, CreateOp, DropOp, IndexOp}
import ch.unibas.dmi.dbis.adam.http.grpc.adam._
import ch.unibas.dmi.dbis.adam.index.structures.IndexStructures
import ch.unibas.dmi.dbis.adam.query.distance.NormBasedDistanceFunction

import scala.concurrent.Future

/**
  * adamtwo
  *
  * Ivan Giangreco
  * March 2016
  */
class DataDefinitionImpl extends AdamDefinitionGrpc.AdamDefinition {
  override def createEntity(request: EntityNameMessage): Future[AckMessage] = {
    try {
      CreateOp(request.entity)
      Future.successful(AckMessage(code = AckMessage.Code.OK))
    } catch {
      case e: Exception => Future.failed(e)
    }
  }

  override def count(request: EntityNameMessage): Future[AckMessage] = {
    try {
      val count = CountOp(request.entity)
      Future.successful(AckMessage(code = AckMessage.Code.OK, message = count.toString))
    } catch {
      case e: Exception => Future.failed(e)
    }
  }


  override def insert(request: InsertMessage): Future[AckMessage] = ???


  override def index(request: IndexMessage): Future[AckMessage] = {
    try {
      val indextypename = request.indextype match {
        case IndexMessage.IndexType.ecp => IndexStructures.ECP
        case IndexMessage.IndexType.sh => IndexStructures.SH
        case IndexMessage.IndexType.lsh => IndexStructures.LSH
        case IndexMessage.IndexType.vaf => IndexStructures.VAF
        case IndexMessage.IndexType.vav => IndexStructures.VAV
        case _ => null
      }

      if(indextypename == null){
        throw new Exception("No index type name given.")
      }


      IndexOp(request.entity, indextypename, NormBasedDistanceFunction(request.norm),  request.options )
      Future.successful(AckMessage(code = AckMessage.Code.OK))
    } catch {
      case e: Exception => Future.failed(e)
    }
  }

  override def dropEntity(request: EntityNameMessage): Future[AckMessage] = {
    try {
      DropOp(request.entity)
      Future.successful(AckMessage(code = AckMessage.Code.OK))
    } catch {
      case e: Exception => Future.failed(e)
    }
  }


  override def dropIndex(request: IndexNameMessage): Future[AckMessage] ={
    try {
      val count = CountOp(request.entity)
      Future.successful(AckMessage(code = AckMessage.Code.OK, message = count.toString))
    } catch {
      case e: Exception => Future.failed(e)
    }
  }
}