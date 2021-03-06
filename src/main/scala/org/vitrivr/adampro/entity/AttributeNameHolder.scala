package org.vitrivr.adampro.entity

import org.vitrivr.adampro.entity.Entity.{AttributeName}

/**
  * ADAMpro
  *
  * Ivan Giangreco
  * February 2017
  */
case class AttributeNameHolder(originalName: String) {
  override def toString = cleanName(originalName)

  override def canEqual(a: Any) = a.isInstanceOf[AttributeNameHolder] || a.isInstanceOf[String]

  override def equals(that: Any): Boolean =
    that match {
      case that: AttributeNameHolder => that.canEqual(this) && toString.equals(that.toString)
      case that : String => that.canEqual(this) && originalName.equals(cleanName(that))
      case _ => false
    }

  override def hashCode: Int = originalName.hashCode

  /**
    *
    * @param str name of attribute
    * @return
    */
  private def cleanName(str : String): String = str.replaceAll("[^A-Za-z0-9_-]", "").toLowerCase()
}

object AttributeNameHolder {
  implicit def toString(name: AttributeName): String = name.toString
  implicit def fromString(str: String): AttributeName = AttributeNameHolder(str)
}
