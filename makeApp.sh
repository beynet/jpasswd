#!/bin/bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-9/Contents/Home/

mvn clean package -Dmaven.test.skip=true && \
$JAVA_HOME/bin/javapackager -deploy -native image -srcdir target -srcfiles jpasswd-1.0.3-jar-with-dependencies.jar -appclass org.beynet.gui.Main -name jpasswd -outdir ./target/app -outfile jpasswd && \
cd target/app/ && \
hdiutil create -size 300m -fs HFS+ -volname "jpasswd" jpasswd-w.dmg && \
DEVS=$(hdiutil attach jpasswd-w.dmg | cut -f 1) && \
DEV=$(echo $DEVS | cut -f 1 -d ' ') && \
{
echo "$DEV"
cp -rf jpasswd.app /Volumes/jpasswd/
hdiutil detach $DEV
hdiutil convert jpasswd-w.dmg -format UDZO -o jpasswd.dmg
echo "ok"
}