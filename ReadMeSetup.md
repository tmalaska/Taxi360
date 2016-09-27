#Run Book

#Build
mvn clean package

#Upload to cluster
scp target/Taxi360.jar root@ted-malaska-hadoop-world-1.vpc.cloudera.com:./
scp data/yellow_tripdata_2009-01.100.csv root@ted-malaska-hadoop-world-1.vpc.cloudera.com:./
scp data/yellow_tripdata_2009-01.10000.csv root@ted-malaska-hadoop-world-1.vpc.cloudera.com:./

#Log into Cluster
ssh root@ted-malaska-hadoop-world-1.vpc.cloudera.com

#Set up Kafka Topic
kafka-topics --zookeeper ted-malaska-hadoop-world-1.vpc.cloudera.com:2181 \
--partition 2 --replication-factor 2 --create --topic taxi-trip-input

#Listen to Kafka with Console 
kafka-topics --zookeeper ted-malaska-hadoop-world-1.vpc.cloudera.com:2181 --list

kafka-console-producer --broker-list \
ted-malaska-hadoop-world-4.vpc.cloudera.com:9092,ted-malaska-hadoop-world-5.vpc.cloudera.com:9092 \
--topic taxi-trip-input

kafka-console-consumer \
--zookeeper ted-malaska-hadoop-world-1.vpc.cloudera.com:2181 \
--topic taxi-trip-input

#Running the generator
java -cp Taxi360.jar com.cloudera.sa.taxi360.common.CsvKafkaPublisher \
ted-malaska-hadoop-world-4.vpc.cloudera.com:9092,ted-malaska-hadoop-world-5.vpc.cloudera.com:9092 \
taxi-trip-input \
yellow_tripdata_2009-01.10000.csv \
10 \
0 \
10 \
async \
1000 \
100

java -cp Taxi360.jar com.cloudera.sa.taxi360.common.CsvKafkaPublisher \
ted-malaska-hadoop-world-4.vpc.cloudera.com:9092,ted-malaska-hadoop-world-5.vpc.cloudera.com:9092 \
taxi-trip-input \
yellow_tripdata_2016-01.csv \
10 \
0 \
10 \
async \
1000 \
100

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

spark-submit --class com.cloudera.sa.taxi360.streaming.ingestion.solr.SparkStreamingTaxiTripToSolR \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
Taxi360.jar \
ted-malaska-hadoop-world-4.vpc.cloudera.com:9092,ted-malaska-hadoop-world-5.vpc.cloudera.com:9092 \
taxi-trip-input \
tmp/checkpoint \
1 \
c \
taxi-trip-collection \
ted-malaska-hadoop-world-1.vpc.cloudera.com:2181/solr

##Test SolR input
Go to the Hue Dashboard Page

#Spark Streaming to Kudu
##Run Spark to Kudu
export JAVA_HOME=/opt/jdk1.8.0_91/

spark-submit --class com.cloudera.sa.taxi360.streaming.ingestion.kudu.SparkStreamingTaxiTripToKudu \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
Taxi360.jar \
ted-malaska-hadoop-world-4.vpc.cloudera.com:9092,ted-malaska-hadoop-world-5.vpc.cloudera.com:9092 \
taxi-trip-input \
1 \
c \
ted-malaska-hadoop-world-2.vpc.cloudera.com \
ny_taxi_entity \
ny_taxi_trip \
tmp/checkpoint

##Test Kudu input
select * from customer_tran_kudu;

##Rest Server
java -cp Taxi360.jar  com.cloudera.sa.taxi360.server.kudu.KuduRestServer 4242 \
ted-malaska-hadoop-world-1.vpc.cloudera.com ny_taxi_trip ny_taxi_entity

##Run Spark SQL & MlLib Example
spark-submit --class com.cloudera.sa.taxi360.etl.machinelearning.kudu.MlLibOnKudu \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
Taxi360.jar \
c \
ted-malaska-hadoop-world-2.vpc.cloudera.com \
ny_taxi_trip \
3 \
3

#Spark Streaming to HBase
##Run Spark to HBase
export JAVA_HOME=/opt/jdk1.8.0_91/
spark-submit --class com.cloudera.sa.taxi360.streaming.ingestion.hbase.SparkStreamingTaxiTripToHBase \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
Taxi360.jar \
ted-malaska-hadoop-world-4.vpc.cloudera.com:9092,ted-malaska-hadoop-world-5.vpc.cloudera.com:9092 \
taxi-trip-input \
1 \
c \
taxi-trip \
6 \
tmp/checkpoint \
/opt/cloudera/parcels/CDH/lib/hbase/conf/

##Test Kudu input
scan 'taxi-trip', {'LIMIT' => 5}

##Rest Server
java -cp Taxi360.jar com.cloudera.sa.taxi360.server.hbase.HBaseRestServer \
4242 \
/opt/cloudera/parcels/CDH/lib/hbase/conf/  \
6 \
taxi-trip

##Rest Server Examples
curl http://ted-malaska-hadoop-world-1.vpc.cloudera.com:4242/rest/hello

curl http://ted-malaska-hadoop-world-1.vpc.cloudera.com:4242/rest/vender/0CMT/timeline

curl http://ted-malaska-hadoop-world-1.vpc.cloudera.com:4242/rest/vender/14VTS/timeline?startTime=0&endTime=1430944160000

0000:0CMT:00000000000
0000:0CMT:1233115446000                                                                                                                          
0000:0CMT:1233115446000
0000:0CMT:1430944160000

#Spark created HDFS Nested Table
spark-submit --class com.cloudera.sa.taxi360.sql.kudu.KuduToNestedHDFS \
--master yarn --deploy-mode client --executor-memory 512MB --num-executors 2 --executor-cores 1 \
Taxi360.jar \
l \
ted-malaska-hadoop-world-2.vpc.cloudera.com \
ny_taxi_trip \
ny_taxi_nested