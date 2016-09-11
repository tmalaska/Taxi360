#Install CM 
ssh root@ted-training-1-1.vpc.cloudera.com

wget http://archive.cloudera.com/cm5/installer/latest/cloudera-manager-installer.bin

chmod u+x cloudera-manager-installer.bin

sudo ./cloudera-manager-installer.bin

#Go to CM to set up
ted-training-1-1.vpc.cloudera.com:7180

#Download kudu
service cloudera-scm-server stop
wget http://archive.cloudera.com/beta/kudu/csd/KUDU-0.9.1.jar
mv KUDU-0.9.1.jar /opt/cloudera/csd/
service cloudera-scm-server start

#Download KuduImplala
https://www.cloudera.com/documentation/betas/kudu/0-5-0/topics/kudu_impala.html#concept_bgs_snk_ft

Impala Service Environment Advanced Configuration Snippet (Safety Valve)
IMPALA_KUDU=1

#Update JDK
service cloudera-scm-server stop

sudo wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u91-b14/jdk-8u91-linux-x64.tar.gz"
sudo tar xzf jdk-8u91-linux-x64.tar.gz
mv jdk1.8.0_91 /opt
cd /opt/jdk1.8.0_91/
sudo alternatives --install /usr/bin/java java /opt/jdk1.8.0_91/bin/java 1
sudo alternatives --config java

sudo alternatives --install /usr/bin/jar jar /opt/jdk1.8.0_91/bin/jar 1
sudo alternatives --install /usr/bin/javac javac /opt/jdk1.8.0_91/bin/javac 1
sudo alternatives --set jar /opt/jdk1.8.0_91/bin/jar
sudo alternatives --set javac /opt/jdk1.8.0_91/bin/javac
sudo ln -sf /opt/jdk1.8.0_91/ /usr/java/latest/

service cloudera-scm-server start

--Update CM Java Home
/opt/jdk1.8.0_91/