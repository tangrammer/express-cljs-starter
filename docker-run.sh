#!/bin/bash

docker build -t rebujito .
docker run -p 3000:3000 rebujito
