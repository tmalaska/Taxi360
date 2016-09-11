package com.cloudera.sa.taxi360.server.hbase

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}

object HBaseGlobalValues {
  var appEventTableName = "app-event"
  var accountMartTableName = "account-mart"
  var numberOfSalts = 10000
  var connection:Connection = null

  def init(conf:Configuration, numberOfSalts:Int,
           appEventTableName:String,
           accountMartTableName:String): Unit = {
    connection = ConnectionFactory.createConnection(conf)
    this.numberOfSalts = numberOfSalts
    this.appEventTableName = appEventTableName
    this.accountMartTableName = accountMartTableName
  }
}
