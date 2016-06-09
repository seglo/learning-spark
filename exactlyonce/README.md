Exactly Once Streaming with Kafka & Cassandra
=============================================

# Developer Setup

## Pre-requisites

1. Install `docker` and `docker-compose` (developed against version 1.11.1 and 1.6.2 respectively).  NOTE: When on linux add yourself to the docker group so you don't require `sudo` to execute these commands.
2. Confirm `ADVERTISED_HOST` environment variable matches your docker host IP in `kafka` service located in `./demo/docker/sbt-docker-compose.yml`.  You can find out this IP with the command `ifconfig docker0`

Now you can bring the docker services up and perform ad hoc operations against them, or run the automated tests.

## Ad-hoc operations

This project uses the [`sbt-docker-compose`](https://github.com/Tapad/sbt-docker-compose) plugin to make it easier to run automated integration tests using services defined in `docker/sbt-docker-compose.yml`.

To start up the services for the project.

`sbt dockerComposeUp`

To stop:

`sbt dockerComposeStop`

To apply the schema to the Cassandra instance you can execute `cqlsh` in the container to get a shell.  Then you can define the keyspace and tables.  Use `docker ps` or inspect the results of `sbt dockerComposeUp` to get the container ID.

`docker exec -it [CONTAINER_ID] cqlsh`

### C* Schema

Define the keyspace and tables:

```
DROP KEYSPACE IF EXISTS exactlyonce;
CREATE KEYSPACE IF NOT EXISTS exactlyonce WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 };
CREATE TABLE IF NOT EXISTS exactlyonce.events (
    id bigint,
    ts timestamp,
    partition int,
    offset bigint,
    source text,
    client text,
    health int,
    PRIMARY KEY (partition, offset)
) WITH CLUSTERING ORDER BY (offset DESC);
CREATE TABLE IF NOT EXISTS exactlyonce.alerts (
    ts timestamp,
    source text,
    event_count int,
    PRIMARY KEY (source, ts));
TRUNCATE exactlyonce.events;
TRUNCATE exactlyonce.alerts;
```

## Integration Tests

To run automated tests (make sure services are stopped before you do this)

`sbt dockerComposeTest`

## Run in IntelliJ manually

1. Bring up docker services `sbt dockerComposeUp`
2. Create C* schema (schema is above)
3. Create a run configuration in IntelliJ: Run -> Edit Configurations -> "Add New Configuration" (+) -> Application -> Enter (including double quotes): `"run-main com.seglo.learningspark.exactlyonce.EventStreamingApp"` -> Add VM parameter: `-Dspark.master=local[*] -Dspark.cassandra.connection.host=<DOCKER-HOST-IP>` -> Add Program argument `<DOCKER-HOST-IP>:9092`
4. Run EventStreamingApp using configuration created in step 3 and TestMessageGenerator in parallel
5. Stop docker services `sbt dockerComposeStop`

# Cluster

## Configure & submit job to YARN

1. Build a fat jar: `sbt assembly`
2. `rsync` to a node on the cluster with spark client utilities. Ex)

`rsync -v -e "ssh -i ~/.ssh/myprivatekey.pem" -a ./target/scala-2.10/exactlyonce-1.0.jar centos@[HOST/IP OF SPARK CLIENT]:/jobs/`

3. Create C* schema (schema is above)
4. Create Kafka topic: exactlyonce.events Kafka topic

`./bin/kafka-topics.sh --create --topic exactlyonce.events --partitions 3 --replication-factor 3 --zookeeper <ZOOKEEPER-IP>:2181`

5. Run `spark-submit` with the job.

`./bin/spark-submit --class com.seglo.learningspark.exactlyonce.EventStreamingApp --master yarn-client --conf spark.cassandra.connection.host=<CASSANDRA-SEED-IP> /demo/exactlyonce-1.0.jar <KAFKA-BROKER-IP>:6667`

4. Start `TestMessageGenerator` to produce simulated events onto Kafka

`java -cp ./exactlyonce-1.0.jar com.seglo.learningspark.exactlyonce.TestMessageGenerator <KAFKA-BROKER-IP>:6667 exactlyonce.events`

# Run the spark-shell on YARN

CMD: `./bin/spark-shell --master yarn-client --driver-memory 512m --executor-memory 512m --jars /jobs/exactlyonce-1.0.jar`

NOTES: Don't pass in the cassandra-connector package because it will conflict with hadoop libs
       on the guava dep.  Instead, just pass the "fat" jar created with the sbt assembly plugin and
       then pass the automatically created SparkContext (sc) into SparkDemoApp.testRdd(sc) to return
       a CassandraRdd

# Reference

### Docker & Docker Compose Command Reference

Run docker-compose (-d to detach).  NOTE: This will not work with ./docker/sbt-docker-compose.yml

`docker-compose up -d`

To attach back to containers to see output/logs:

`docker-compose logs`

Run cqlsh on the cassandra container.

`docker exec -it [CONTAINER_ID] cqlsh`

Run kafka-console-consumer on kafka

`docker exec -it [CONTAINER_ID] /opt/kafka_2.11-0.8.2.1/bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic test --from-beginning`

Open bash shell

`docker exec -it [CONTAINER_ID] /bin/bash`

Publish the modified spotify/kafka image (upgraded kafka to 0.9.0.1).  sbt-docker-compose doesn't support `build:` param, so to use it we have to build and publish it locally first.

`docker build -t kafka09 ./demo/docker/containers/kafka09/kafka`
