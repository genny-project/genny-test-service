#!/bin/bash
kafkacat -b kafka:9092 -C -o beginning -q -t $1 
# kcat -b kafka:9092 -C -o beginning -q -t search_events 


