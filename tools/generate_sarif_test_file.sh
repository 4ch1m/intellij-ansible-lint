#!/bin/bash

# Pulls an up-to-date version of 'ansible-lint' and generates
# the parseable SARIF file (which can be used for unit testing).

SCRIPT_PATH=$(dirname $(realpath "${0}"))

PLAYBOOK_FILE="test-playbook.yml"
SARIF_OUTPUT_FILE="sarif_result.json"

LINT_CMD+="ansible-lint "
LINT_CMD+="-q "
LINT_CMD+="--parseable "
LINT_CMD+="--format sarif "
LINT_CMD+="${PLAYBOOK_FILE} "

mkdir --parents "${SCRIPT_PATH}/output"
touch "${SCRIPT_PATH}/output/${SARIF_OUTPUT_FILE}"

docker \
  run \
  --interactive \
  --tty \
  --rm \
  --volume "${SCRIPT_PATH}/${PLAYBOOK_FILE}":/tmp/${PLAYBOOK_FILE} \
  --volume "${SCRIPT_PATH}/output":/tmp/output \
  --workdir /tmp \
  ghcr.io/ansible/community-ansible-dev-tools:latest \
  /bin/bash -c "${LINT_CMD} | python -m json.tool | tee /tmp/output/${SARIF_OUTPUT_FILE}"

cp "${SCRIPT_PATH}/output/${SARIF_OUTPUT_FILE}" \
   "${SCRIPT_PATH}/../src/test/resources/${SARIF_OUTPUT_FILE}"
