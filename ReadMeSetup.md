#Run Book

#Build
mvn clean package

#Upload to cluster
scp target/Taxi360.jar root@ted-cap1-mergejoin-tests-1.vpc.cloudera.com:./

#Log into Cluster
ssh root@ted-cap1-mergejoin-tests-1.vpc.cloudera.com

#Set up Kafka Topic
kafka-topics --zookeeper ted-cap1-mergejoin-tests-1.vpc.cloudera.com:2181 \
--partition 2 --replication-factor 2 --create --topic taxi-trip-input

#Listen to Kafka with Console 
kafka-topics --zookeeper ted-cap1-mergejoin-tests-1.vpc.cloudera.com:2181 --list

kafka-console-producer --broker-list \
ted-cap1-mergejoin-tests-4.vpc.cloudera.com:9092,ted-cap1-mergejoin-tests-5.vpc.cloudera.com:9092 \
--topic taxi-trip-input

#Running the generator
java -cp Taxi360.jar com.cloudera.sa.taxi360.common.CsvKafkaPublisher \
ted-cap1-mergejoin-tests-4.vpc.cloudera.com:9092,ted-cap1-mergejoin-tests-5.vpc.cloudera.com:9092 \
taxi-trip-input 100 0 5 async 1000 10000 20000 ./data/uber_bay_area_lat_lon.csv 1000

"<brokerList> " +
        "<topicName> " +
        "<dataFolderOrFile> " +
        "<sleepPerRecord> " +
        "<acks> " +
        "<linger.ms> " +
        "<producer.type> " +
        "<batch.size> <salts>"

kafka-console-consumer \
--zookeeper ted-cap1-mergejoin-tests-1.vpc.cloudera.com:2181 \
--topic taxi-trip-input

#Set up Kudu Tables
Run these in HUE in the impala window

kudu/taxi360/create_ny_taxi_trip_table.impala.sql
kudu/taxi360/create_ny_taxi_entity_table.impala.sql

#Set up SolR Collection
solrctl instancedir --generate taxi-trip-collection

switch out the schema.xml with the following file taxi-trip-collection/conf/schema.xml

solrctl instancedir --create taxi-trip-collection taxi-trip-collection

solrctl collection --create taxi-trip-collection -s 3 -r 2 -m 3

#Set up HBase Table
hadoop jar Taxi360.jar  com.cloudera.sa.taxi360.setup.hbase.CreateSaltedTable taxi-trip f 6 6 /opt/cloudera/parcels/CDH/lib/hbase/conf/

#Spark Streaming to SolR
##Run Spark to SolR
export JAVA_HOME=/opt/jdk1.8.0_91/

spark-submit --class com.cloudera.sa.apptrans.streaming.ingestion.solr.SparkStreamingAppEventToSolR \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
Taxi360.jar \
ted-cap1-mergejoin-tests-4.vpc.cloudera.com:9092,ted-cap1-mergejoin-tests-5.vpc.cloudera.com:9092 \
taxi-trip-input \
tmp/checkpoint \
1 \
c \
taxi-trip-collection \
ted-cap1-mergejoin-tests-1.vpc.cloudera.com:2181/solr

##Test SolR input
Go to the Hue Dashboard Page

#Spark Streaming to Kudu
##Run Spark to Kudu
export JAVA_HOME=/opt/jdk1.8.0_91/

spark-submit --class com.cloudera.sa.taxi360.streaming.ingestion.solr.SparkStreamingAppEventToKudu \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
AppTrans.jar \
ted-cap1-mergejoin-tests-4.vpc.cloudera.com:9092,ted-cap1-mergejoin-tests-5.vpc.cloudera.com:9092 \
app-event-input \
1 \
c \
ted-cap1-mergejoin-tests-1.vpc.cloudera.com \
ny_taxi_entity \
ny_taxi_trip \
tmp/checkpoint

##Test Kudu input
select * from customer_tran_kudu;

##Rest Server
com.cloudera.sa.taxi360.server.kudu.KuduRestServer 4242 \
ted-cap1-mergejoin-tests-1.vpc.cloudera.com ny_taxi_trip ny_taxi_entity

!!!
##Run Spark SQL Example
spark-submit --class com.cloudera.sa.apptrans.sql.kudu.KuduSimpleSums \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
AppTrans.jar \
c \
ted-training-1-2.vpc.cloudera.com \
account_mart_kudu

#Spark Streaming to HBase
##Run Spark to HBase
export JAVA_HOME=/opt/jdk1.8.0_91/
spark-submit --class com.cloudera.sa.taxi360.streaming.ingestion.hbase.SparkStreamingAppEventToHBase \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
AppTrans.jar \
ted-cap1-mergejoin-tests-4.vpc.cloudera.com:9092,ted-cap1-mergejoin-tests-5.vpc.cloudera.com:9092 \
app-event-input \
1 \
c \
app-event \
6 \
tmp/checkpoint \
/opt/cloudera/parcels/CDH/lib/hbase/conf/

##Test Kudu input
scan 'card-trans'

##Rest Server
com.cloudera.sa.example.card.server.hbase.HBaseRestService \
4242 /
/opt/cloudera/parcels/CDH/lib/hbase/conf/  \
6 \
customer_trans_kudu






