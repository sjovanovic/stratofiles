# StatoFiles File Server

Simple file server that uses Apache Cassandra to store files.

As a client library it uses [Astyanax](https://github.com/Netflix/astyanax "by Netflix") and it's [Chunked Object Store](https://github.com/Netflix/astyanax/wiki/Chunked-Object-Store) implementation.

## Prerequisites

- Apache Cassandra running on a same machine.
- Cassandra version should be [3.2.1](http://www.apache.org/dyn/closer.lua/cassandra/3.2.1/apache-cassandra-3.2.1-bin.tar.gz)
- make sure that thrift rpc server is on (`start_rpc: true` in `cassandra.yaml`)
- also `cluster_name: 'Test Cluster'` in `cassandra.yaml`

## Build

Since this is a regular Maven web app based on maven-archetype-webapp it can be built with `mvn package` however there's a convenience `./make.sh` script as well. 
This will build a war file in the target directory.

## Run
Just use the war file and run it inside a java web container. 

Alternatively there's a `./run.sh` script that will run it via included `jetty-runner.jar`, this is mainly for test purposes. 

## Initial Setup

First time the server is run on empty Cassandra open:
`GET /setup` (e.g. http://localhost:8080/setup). This will create a column family needed for storage.

## Usage

Currently there are only two methods: one to upload a file, other to download

#### Upload file

```
POST /upload?file=example.png 

---binary data ---
```

CURL example:

```
curl -XPOST "http://localhost:8080/upload?file=example.png" --data-binary "@example.png"
```


#### Download file

```
GET /download?file=example.png
```

CURL example:

```
curl -XGET "http://localhost:8080/download?file=example.png" > example.png
```

## Misc

Everything related to file upload and download is in [IndexController.java](ViewPointStorage/src/main/java/net/project/controller/IndexController.java)

 
