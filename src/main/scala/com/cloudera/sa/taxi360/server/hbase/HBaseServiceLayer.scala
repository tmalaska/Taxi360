package com.cloudera.sa.taxi360.server.hbase

import javax.ws.rs._
import javax.ws.rs.core.MediaType

import com.cloudera.sa.taxi360.model.NyTaxiYellowTrip
import com.cloudera.sa.taxi360.streaming.ingestion.hbase.TaxiTripHBaseHelper
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{ConnectionFactory, Scan}

import scala.collection.mutable

@Path("rest")
class HBaseServiceLayer {

  @GET
  @Path("hello")
  @Produces(Array(MediaType.TEXT_PLAIN))
  def hello(): String = {
    "Hello World"
  }

  @GET
  @Path("vender/{venderId}/timeline")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def getTripTimeLine (@PathParam("venderId") venderId:String,
                          @QueryParam("startTime") startTime:String = Long.MaxValue.toString,
                          @QueryParam("endTime")  endTime:String = Long.MinValue.toString): List[NyTaxiYellowTrip] = {

    val table = HBaseGlobalValues.connection.getTable(TableName.valueOf(HBaseGlobalValues.appEventTableName))

    val scan = new Scan()
    scan.setStartRow(TaxiTripHBaseHelper.generateRowKey(venderId, startTime.toLong, HBaseGlobalValues.numberOfSalts))
    scan.setStopRow(TaxiTripHBaseHelper.generateRowKey(venderId, endTime.toLong, HBaseGlobalValues.numberOfSalts))


    val scannerIt = table.getScanner(scan).iterator()

    val tripList = new mutable.MutableList[NyTaxiYellowTrip]

    while(scannerIt.hasNext) {
      val result = scannerIt.next()
      tripList += TaxiTripHBaseHelper.convertToTaxiTrip(result)
    }

    tripList.toList
  }

}
