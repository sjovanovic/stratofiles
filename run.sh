#!/bin/bash

# usage: run.sh path/to/file.war

cd ViewPointStorage

WAR_FILE=$1
if [ -z "$1" ]
  then
    WAR_FILE="target/ViewPointStorage.war"
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -jar "$DIR/../jetty/jetty-runner.jar" "`pwd`/$WAR_FILE"

cd ../