#!/bin/bash
#
# This script is used to start the server from a supplied config file
#

export SVR_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "** starting from ${SVR_HOME} **"

echo server home = $SVR_HOME
#exit

#cd ${SVR_HOME}

JAVA_MAIN='gash.router.client.ReadQueueUp'



# superceded by http://ww.oracle.com/technetwork/java/tuning-139912.html
JAVA_TUNE='-client -Djava.net.preferIPv4Stack=true'


JAVA_ARGS=""
#echo -e "\n** config: ${JAVA_ARGS} **\n"

java ${JAVA_TUNE} -cp .:${SVR_HOME}/lib/'*':${SVR_HOME}/classes ${JAVA_MAIN}