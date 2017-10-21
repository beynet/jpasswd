#!/bin/bash
$JAVA_HOME/bin/javapackager -deploy -native image -srcfiles ./target/jpasswd-1.0.2-jar-with-dependencies.jar -appclass org.beynet.gui.Main -name jpasswd -outdir ./target/app -outfile jpasswd
