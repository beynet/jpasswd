#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu11.45.27-ca-jdk11.0.10-macosx_x64/zulu-11.jdk/Contents/Home
export JPACKAGE=/Library/Java/JavaVirtualMachines/jdk-14.0.1.jdk/Contents/Home/bin/jpackage


mvn clean package -Dmaven.test.skip=true && \
$JPACKAGE --runtime-image $JAVA_HOME  -n jpasswd --main-class org.beynet.gui.Main --main-jar jpasswd-1.0.8-SNAPSHOT-jar-with-dependencies.jar --input ./target
