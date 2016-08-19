package ch.unibas.dmi.dbis.adam.evaluation.utils

import java.io.{BufferedWriter, FileWriter, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Calendar

/**
  * Created by silvan on 15.08.16.
  */
object PartitionResultLogger {

  /** stub */
  def init : Unit = {}
  private val out = new PrintWriter(new BufferedWriter(new FileWriter("part_" + new SimpleDateFormat("MMdd_HHmm").format(Calendar.getInstance.getTime) + ".tsv", true)))
  private val seperator = "\t"
  private val names = Seq("index", "tuples", "dimensions", "partitions", "partitioner", "distribution")

  /** Header */
  out.println("curr_time" + seperator + names.mkString(seperator))
  out.flush()

  def write(values: Map[String, Any]): Unit = {
    out.println(Calendar.getInstance.getTime + seperator + names.map(values(_)).mkString(seperator))
    out.flush()
  }

}
