#!/bin/bash

SCRIPT_PATH=$(dirname $(realpath "${0}"))

if [ ! -f "${SCRIPT_PATH}/output/sarif_result.json" ]; then
   bash "${SCRIPT_PATH}/generate_sarif_test_file.sh"
fi

SCHEMA_URL=$(jq '.["$schema"]' "${SCRIPT_PATH}/output/sarif_result.json" | tr -d '"')

curl \
  --silent \
  --location \
  "${SCHEMA_URL}" \
  | dos2unix > "${SCRIPT_PATH}/../src/main/resources/json/sarif.json"
