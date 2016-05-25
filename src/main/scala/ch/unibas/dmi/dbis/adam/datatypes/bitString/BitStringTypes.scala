package ch.unibas.dmi.dbis.adam.datatypes.bitString

/**
 * adamtwo
 *
 * Ivan Giangreco
 * October 2015
 */
object BitStringTypes {
  sealed abstract class BitStringType(val num : Byte, val factory : BitStringFactory)
  case object MBS extends BitStringType(0, MinimalBitString)
}