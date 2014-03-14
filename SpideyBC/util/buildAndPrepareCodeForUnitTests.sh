#!/bin/bash

set -e

JOPROOT="/home/andrease/Dokumenter/phD/githubJOP/jop/"


#TEST="SimpleLoop3"
#echo $TEST
#cd $JOPROOT
#make P1=test P2=wcet P3=$TEST java_app
#cd -
#python extractSourceFromJopRepos.py --joproot $JOPROOT --srctarget "/tmp/${TEST}src" --mapping com:common/com java:jdk_base/java joprt:common/joprt util:common/util wcet:test/wcet
#rm -rf "${TEST}src/"
#cp -R "/tmp/${TEST}src/" .
#cp "${JOPROOT}/java/target/dist/lib/classes.jar" "${TEST}.jar"
#
#
#TEST="SimpleLoop4"
#echo $TEST
#cd $JOPROOT
#make P1=test P2=wcet P3=$TEST java_app
#cd -
#python extractSourceFromJopRepos.py --joproot $JOPROOT --srctarget "/tmp/${TEST}src" --mapping com:common/com java:jdk_base/java joprt:common/joprt util:common/util wcet:test/wcet
#rm -rf "${TEST}src/"
#cp -R "/tmp/${TEST}src/" .
#cp "${JOPROOT}/java/target/dist/lib/classes.jar" "${TEST}.jar"
#
#
#TEST="NestedLoop"
#echo $TEST
#cd $JOPROOT
#make P1=test P2=wcet P3=$TEST java_app
#cd -
#python extractSourceFromJopRepos.py --joproot $JOPROOT --srctarget "/tmp/${TEST}src" --mapping com:common/com java:jdk_base/java joprt:common/joprt util:common/util wcet:test/wcet
#rm -rf "${TEST}src/"
#cp -R "/tmp/${TEST}src/" .
#cp "${JOPROOT}/java/target/dist/lib/classes.jar" "${TEST}.jar"

#Current test cases
TEST="pmFFTcpResult"
echo $TEST
cd $JOPROOT
make java_app -e P1=paper P2=privmem/pmFFTcpResult P3=pmFFTcpResult
cd -
python extractSourceFromJopRepos.py --joproot $JOPROOT --srctarget "/tmp/${TEST}" --mapping com:common/com java:jdk_base/java joprt:common/joprt util:common/util javax:rtapi/javax privmem:paper/privmem ALTjava:jdk16/java
rm -rf "${TEST}/"
cp -R "/tmp/${TEST}/" .
cp "${JOPROOT}/java/target/dist/lib/classes.jar" "${TEST}.jar"


#TEST="Sorter"
#echo $TEST
#cd $JOPROOT
#make java_app -e P1=paper P2=privmem/sorter P3=SorterApp 
#cd -
#python extractSourceFromJopRepos.py --joproot $JOPROOT --srctarget "/tmp/${TEST}" --mapping com:common/com java:jdk_base/java ALTjava:jdk16/java joprt:common/joprt util:common/util javax:rtapi/javax privmem:paper/privmem 
#rm -rf "${TEST}/"
#cp -R "/tmp/${TEST}/" .
#cp "${JOPROOT}/java/target/dist/lib/classes.jar" "${TEST}.jar"

