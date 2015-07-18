#!/bin/bash

# source server header
source server-header.sh

# Additional settings
configs=$( ls ${conf}/02285* )
resultPage=false;
testServerMode=false;

runServer
