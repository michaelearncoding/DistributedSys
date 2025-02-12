#!/bin/bash

#
# basic test script
#

rm -rf mr-tmp
mkdir mr-tmp || exit 1
cd mr-tmp || exit 1
rm -f mr-*

# run word count test
../mrcoordinator ../pg*txt &
sleep 1

../mrworker ../wc.so &
../mrworker ../wc.so &
../mrworker ../wc.so &

wait
