#!/usr/bin/env bash

declare LOCATION

LOCATION=$(dirname $0)

$LOCATION/ant.sh "$@" build-war
