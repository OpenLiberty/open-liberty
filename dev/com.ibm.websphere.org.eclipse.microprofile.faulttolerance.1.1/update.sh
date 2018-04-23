#!/bin/sh -ex

# Simple shell script to update this project to contain the latest MP Fault Tolerance api

git clone https://github.com/eclipse/microprofile-fault-tolerance.git -b master --single-branch --depth 1
rm -r src/* || true
cp -r microprofile-fault-tolerance/api/src/main/java/* src/
rm -rf microprofile-fault-tolerance
