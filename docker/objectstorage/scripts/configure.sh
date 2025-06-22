#!/bin/sh

alias garage="docker exec garaged /garage"

echo "========================="
echo "STARTING CONFIGURATION";
echo "========================="

if [ "$(docker exec garaged /garage bucket list | grep -c civicsage-bucket)" -eq 1 ]; then
  echo "BUCKET ALREADY PRESENT. NOTHING TO DO."
else
  echo "CONFIGURING NODE"
  NODE_ID=$(garage status | awk '/^==== HEALTHY NODES ====/ {found=1; next;next} found && NF {if (++line == 2) {print $1; exit}}')

  garage layout assign -z dc1 -c 1G "$NODE_ID"
  garage layout apply --version 1
  echo "NODE CONFIGURATION DONE"

  echo "========================="

  echo "CREATING BUCKET"
  garage bucket create civicsage-bucket
  garage bucket list
  garage bucket info civicsage-bucket


  echo "CREATING KEY"
  # creating the key to access it
  output=$(garage key create civicsage-app-key)
  KEY_ID=$(echo "$output" | awk -F': *' '/Key ID:/ {print $2}')
  SECRET_KEY=$(echo "$output" | awk -F': *' '/Secret key:/ {print $2}')
  garage key list
  garage key info civicsage-app-key

  echo "GRANTING KEY ACCESS TO BUCKET"
  garage bucket allow \
    --read \
    --write \
    --owner \
    civicsage-bucket \
    --key civicsage-app-key

  echo "FINAL SUMMARY"
  garage bucket info civicsage-bucket
  echo "================"
  echo "================"
  echo "THE FOLLOWING KEY HAS BEEN GENERATED"
  echo "================"
  echo "KEY ID: $KEY_ID"
  echo "SECRET_KEY: $SECRET_KEY"
fi