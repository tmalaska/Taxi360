package com.cloudera.sa.taxi360.sql.kudu

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

object KuduToHDFS {
  def main(args: Array[String]): Unit = {

    if (args.length == 0) {
      println("Args: <runLocal> <kuduMaster> " +
        "<kuduAccountMartTableName> " +
        "<hdfsAccountMartTableName> " +
        "<numOfCenters> " +
        "<numOfIterations> ")
      return
    }

    val runLocal = args(0).equalsIgnoreCase("l")
    val kuduMaster = args(1)
    val kuduAccountMartTableName = args(2)
    val hdfsAccountMartTableName = args(3)
    val numOfCenters = args(4).toInt
    val numOfIterations = args(5).toInt

    val sc: SparkContext = if (runLocal) {
      val sparkConfig = new SparkConf()
      sparkConfig.set("spark.broadcast.compress", "false")
      sparkConfig.set("spark.shuffle.compress", "false")
      sparkConfig.set("spark.shuffle.spill.compress", "false")
      new SparkContext("local", "TableStatsSinglePathMain", sparkConfig)
    } else {
      val sparkConfig = new SparkConf().setAppName("TableStatsSinglePathMain")
      new SparkContext(sparkConfig)
    }

    val hiveContext = new HiveContext(sc)

    val kuduOptions = Map(
      "kudu.table" -> kuduAccountMartTableName,
      "kudu.master" -> kuduMaster)

    hiveContext.read.options(kuduOptions).format("org.kududb.spark.kudu").load.
      registerTempTable("account_mart_tmp")

    hiveContext.sql("CREATE TABLE " + hdfsAccountMartTableName + " AS SELECT * FROM account_mart_tmp")

    sc.stop()
  }
}
