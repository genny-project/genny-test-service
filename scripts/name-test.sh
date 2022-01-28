#!/bin/bash
RESPONSE=`curl -s -X POST localhost:8099/api/test/multi/random/$1`
echo "============= ITEMS ==============="
echo `echo "$RESPONSE" | jq -r .items`