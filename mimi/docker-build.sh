#!/bin/bash

lein clean
lein cljsbuild once prod

eval $(docker-machine env default)
docker build -t swarmapps/mimi .
