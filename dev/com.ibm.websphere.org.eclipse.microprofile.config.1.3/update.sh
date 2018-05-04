#!/bin/sh -ex

# Simple shell script to update this project to contain the latest MP Config api

git clone https://github.com/eclipse/microprofile-config.git -b master --single-branch --depth 1
rm -r src/* || true
cp -r microprofile-config/api/src/main/java/* src/
rm -rf microprofile-config
