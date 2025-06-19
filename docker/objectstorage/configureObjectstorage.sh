#!/bin/sh

alias garage="docker exec -ti garaged /garage"
garage status

# garage status | grep -E '^[0-9a-f]{16}' | awk '{print $1}'

# store in variable
NODE_ID=$(garage status | awk '/^==== HEALTHY NODES ====/ {found=1; next;next} found && NF {if (++line == 2) {print $1; exit}}')

garage layout assign -z dc1 -c 1G "$NODE_ID"
garage layout apply --version 1