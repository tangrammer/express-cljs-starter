#!/bin/bash

lein clean
lein cljsbuild once prod
docker build -t swarmapps/mimi .
