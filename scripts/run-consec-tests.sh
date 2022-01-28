#!/bin/bash
echo `curl -s -X GET localhost:8099/api/test/random/$1`
