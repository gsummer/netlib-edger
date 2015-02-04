#!/bin/sh

## Download PathVisio
cd /tmp
wget http://www.pathvisio.org/data/releases/3.1.3/pathvisio_bin-3.1.3-r3968.tar.gz
tar -xvf pathvisio_bin-3.1.3-r3968.tar.gz pathvisio-3.1.3/pathvisio.jar --strip-components 1

## Install maven library
mvn install:install-file -Dfile=pathvisio.jar -DgroupId=org.pathvisio -DartifactId=core -Dversion=3.1.3 -Dpackaging=jar

## Cleanup
rm pathvisio.jar
rm pathvisio_bin-3.1.3-r3968.tar.gz
