#!/bin/bash

printf "latest commit: $(git rev-parse --verify HEAD --short)\n"
curl https://api.swarmloyalty.co.za/starbucks/api/v1/health
curl https://api.swarmloyalty.co.za/mimi/health
printf "\n"
