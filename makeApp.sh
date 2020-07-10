#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-14.0.1.jdk/Contents/Home

mvn clean package -Dmaven.test.skip=true && \
$JAVA_HOME/bin/jpackage --runtime-image $JAVA_HOME  -n jpasswd --main-class org.beynet.gui.Main --main-jar jpasswd-1.0.5-SNAPSHOT-jar-with-dependencies.jar --input ./target