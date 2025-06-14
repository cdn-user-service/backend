#!/bin/bash
CRTDIR=$(cd $(dirname $0); pwd)
echo curTime
cd $CRTDIR
echo $CRTDIR
echo $( ps -ef | grep 'sytvad-2.6.jar' | grep -v grep | awk '{print $2}' ) 
pid=$( ps -ef | grep 'sytvad-2.6.jar' | grep -v grep | awk '{print $2}' ) 
curTime=$(date "+%H%M%S")
echo $pid
kill 9 $pid
echo 3
sleep 1
echo 2
sleep 1
echo 1
sleep 1
nohup java -jar $CRTDIR/sytvad-2.6.jar   > $CRTDIR/cdn_jar_log_$curTime.jlog 2>&1 &