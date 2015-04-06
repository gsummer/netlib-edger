#!/bin/sh
set -e

## Download PathVisio
cd /tmp
wget -nc http://www.pathvisio.org/data/releases/3.1.3/pathvisio_bin-3.1.3-r3968.tar.gz
tar -xvf pathvisio_bin-3.1.3-r3968.tar.gz pathvisio-3.1.3/pathvisio.jar --strip-components 1
cd /tmp/pathvisio-3.1.3/

## Extract all jar files within pathvisio.jar and then add the extracted files
mkdir pathvisio-jars
unzip -q -o pathvisio.jar "*.jar" -d pathvisio-jars
zip -d pathvisio.jar "*.jar"
mkdir pathvisio-unjars
find pathvisio-jars -name "*.jar" -exec unzip -q -o {} -d pathvisio-unjars  \;
cd pathvisio-unjars
rm -R META-INF
zip -r ../pathvisio.jar *
cd ..
rm -R pathvisio-jars
rm -R pathvisio-unjars

## Remove all jar files within pathvisio.jar

find . -name "*.jar" | while read filename; do unzip -o -d "`basename -s .zip "$filename"`" "$filename"; done;


## Install maven library
mvn install:install-file -Dfile=pathvisio.jar -DgroupId=org.pathvisio -DartifactId=core -Dversion=3.1.3 -Dpackaging=jar

## Cleanup
rm pathvisio.jar
rm pathvisio_bin-3.1.3-r3968.tar.gz
