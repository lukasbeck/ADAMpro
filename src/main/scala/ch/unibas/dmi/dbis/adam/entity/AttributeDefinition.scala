package ch.unibas.dmi.dbis.adam.entity

import ch.unibas.dmi.dbis.adam.datatypes.FieldTypes.FieldType
import ch.unibas.dmi.dbis.adam.exception.GeneralAdamException
import ch.unibas.dmi.dbis.adam.storage.{StorageHandler, StorageHandlerRegistry}

/**
  * adamtwo
  *
  * Ivan Giangreco
  * May 2016
  *
  * @param name      name of attribute
  * @param fieldtype type of field
  * @param pk        is primary key
  * @param storagehandlername
  * @param params
  */
case class AttributeDefinition(name: String, fieldtype: FieldType, pk: Boolean = false, private val storagehandlername: Option[String] = None, params: Map[String, String] = Map()) {
  /**
    * Returns the storage handler for the given attribute (it possibly uses a fallback, if no storagehandlername is specified by using the fieldtype)
    */
  lazy val storagehandler: Option[StorageHandler] = {
    val handler = StorageHandlerRegistry.getOrElse(storagehandlername, fieldtype)

    if(!handler.get.supports.contains(fieldtype)){
      throw new GeneralAdamException("storage handler " + storagehandlername.getOrElse("<empty>") + " does not support field type " + fieldtype.name)
    }

    handler
  }
}

