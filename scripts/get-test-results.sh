#!/bin/bash
echo `curl -s -X GET localhost:8099/api/metrics/$1`