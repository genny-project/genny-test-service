#!/bin/bash
echo "Millis:" `curl -s -X GET localhost:8099/api/metrics/$1`