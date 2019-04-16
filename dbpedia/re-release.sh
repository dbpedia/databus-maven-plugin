#!/bin/bash

mvn exec:exec -P download-pre-release

# mvn exec:exec -P rapandsort

mvn versions:set -DnewVersion=$(ls * | grep '^[0-9]\{4\}.[0-9]\{2\}.[0-9]\{2\}$' | sort -u  | tail -1)
