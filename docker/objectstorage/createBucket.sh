#!/bin/sh

alias garage="docker exec -ti garaged /garage"

echo "+++++ Creating bucket +++++"
garage bucket create civicsage-bucket
garage bucket list
garage bucket info civicsage-bucket


echo "+++++ Creating key +++++"
# creating the key to access it
garage key create civicsage-app-key
garage key list
garage key info civicsage-app-key

echo "+++++ Granting access to bucket +++++"
garage bucket allow \
  --read \
  --write \
  --owner \
  civicsage-bucket \
  --key civicsage-app-key

echo "+++++ Final summary +++++"
garage bucket info civicsage-bucket