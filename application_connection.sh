#!/bin/bash


NETWORK_NAME=${TEST_NETWORK_NAME:-test-network}
NS=${TEST_NETWORK_KUBE_NAMESPACE:-${NETWORK_NAME}}
LOG_FILE=${TEST_NETWORK_LOG_FILE:-network.log}
DEBUG_FILE=${TEST_NETWORK_DEBUG_FILE:-network-debug.log}

function logging_init() {
  # Reset the output and debug log files
  printf '' > ${LOG_FILE} > ${DEBUG_FILE}

  # Write all output to the control flow log to STDOUT
  tail -f ${LOG_FILE} &

  # Call the exit handler when we exit.
  trap "exit_fn" EXIT

  # Send stdout and stderr from child programs to the debug log file
  exec 1>>${DEBUG_FILE} 2>>${DEBUG_FILE}

  # There can be a race between the tail starting and the next log statement
  sleep 0.5
}

function exit_fn() {
  rc=$?

  # Write an error icon to the current logging statement.
  if [ "0" -ne $rc ]; then
    pop_fn $rc
  fi

  # always remove the log trailer when the process exits.
  pkill -P $$
}

function push_fn() {
  #echo -ne "   - entering ${FUNCNAME[1]} with arguments $@"

  echo -ne "   - $@ ..." >> ${LOG_FILE}
}

function log() {
  echo -e $@ >> ${LOG_FILE}
}

function pop_fn() {
#  echo exiting ${FUNCNAME[1]}

  local res=$1
  if [ $# -eq 0 ]; then
    echo -ne "\r✅"  >> ${LOG_FILE}
    echo "" >> ${LOG_FILE}
    return
  fi

  local res=$1
  if [ $res -eq 0 ]; then
    echo -ne "\r✅"  >> ${LOG_FILE}

  elif [ $res -eq 1 ]; then
    echo -ne "\r⚠️" >> ${LOG_FILE}

  elif [ $res -eq 2 ]; then
    echo -ne "\r☠️" >> ${LOG_FILE}

  elif [ $res -eq 127 ]; then
    echo -ne "\r☠️" >> ${LOG_FILE}

  else
    echo -ne "\r" >> ${LOG_FILE}
  fi

  echo "" >> ${LOG_FILE}
}


function app_extract_MSP_archives() {
  mkdir -p extra/msp
  set -ex
  kubectl -n $NS exec deploy/org1-ca -- tar zcf - -C /var/hyperledger/fabric organizations/peerOrganizations/org1.example.com/msp | tar zxf - -C extra/msp
  kubectl -n $NS exec deploy/org2-ca -- tar zcf - -C /var/hyperledger/fabric organizations/peerOrganizations/org2.example.com/msp | tar zxf - -C extra/msp

  kubectl -n $NS exec deploy/org1-ca -- tar zcf - -C /var/hyperledger/fabric organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp | tar zxf - -C extra/msp
  kubectl -n $NS exec deploy/org2-ca -- tar zcf - -C /var/hyperledger/fabric organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp | tar zxf - -C extra/msp
}

function app_one_line_pem {
    echo "`awk 'NF {sub(/\\n/, ""); printf "%s\\\\\\\n",$0;}' $1`"
}

function app_json_ccp {
  local ORG=$1
  local PP=$(app_one_line_pem $2)
  local CP=$(app_one_line_pem $3)
  sed -e "s/\${ORG}/$ORG/" \
      -e "s#\${PEERPEM}#$PP#" \
      -e "s#\${CAPEM}#$CP#" \
      ccp-template.json
}

function app_id {
  local MSP=$1
  local CERT=$(app_one_line_pem $2)
  local PK=$(app_one_line_pem $3)

  sed -e "s#\${CERTIFICATE}#$CERT#" \
      -e "s#\${PRIVATE_KEY}#$PK#" \
      -e "s#\${MSPID}#$MSP#" \
      appuser.id.template
}

function construct_application_configmap() {
  push_fn "Constructing application connection profiles"

  app_extract_MSP_archives

  mkdir -p extra/application/wallet
  mkdir -p extra/application/gateways

  local peer_pem=extra/msp/organizations/peerOrganizations/org1.example.com/msp/tlscacerts/org1-tls-ca.pem
  local ca_pem=extra/msp/organizations/peerOrganizations/org1.example.com/msp/cacerts/org1-ecert-ca.pem

  echo "$(app_json_ccp 1 $peer_pem $ca_pem)" > extra/application/gateways/org1_ccp.json

  peer_pem=extra/msp/organizations/peerOrganizations/org2.example.com/msp/tlscacerts/org2-tls-ca.pem
  ca_pem=extra/msp/organizations/peerOrganizations/org2.example.com/msp/cacerts/org2-ecert-ca.pem

  echo "$(app_json_ccp 2 $peer_pem $ca_pem)" > extra/application/gateways/org2_ccp.json

  pop_fn

  push_fn "Getting Application Identities"

  local cert=extra/msp/organizations/peerOrganizations/org1.example.com/users/Admin\@org1.example.com/msp/signcerts/cert.pem
  local pk=extra/msp/organizations/peerOrganizations/org1.example.com/users/Admin\@org1.example.com/msp/keystore/server.key

  echo "$(app_id Org1MSP $cert $pk)" > extra/application/wallet/appuser_org1.id

  local cert=extra/msp/organizations/peerOrganizations/org2.example.com/users/Admin\@org2.example.com/msp/signcerts/cert.pem
  local pk=extra/msp/organizations/peerOrganizations/org2.example.com/users/Admin\@org2.example.com/msp/keystore/server.key

  echo "$(app_id Org2MSP $cert $pk)" > extra/application/wallet/appuser_org2.id

  pop_fn

#  Run in production only
#  push_fn "Creating ConfigMap \"app-fabric-tls-v1-map\" with TLS certificates for the application"
#  kubectl -n $NS delete configmap app-fabric-tls-v1-map || true
#  kubectl -n $NS create configmap app-fabric-tls-v1-map --from-file=./build/msp/organizations/peerOrganizations/org1.example.com/msp/tlscacerts
#  pop_fn
#
#  push_fn "Creating ConfigMap \"app-fabric-ids-v1-map\" with identities for the application"
#  kubectl -n $NS delete configmap app-fabric-ids-v1-map || true
#  kubectl -n $NS create configmap app-fabric-ids-v1-map --from-file=./build/application/wallet
#  pop_fn
#
#  push_fn "Creating ConfigMap \"app-fabric-ccp-v1-map\" with ConnectionProfile for the application"
#  kubectl -n $NS delete configmap app-fabric-ccp-v1-map || true
#  kubectl -n $NS create configmap app-fabric-ccp-v1-map --from-file=./build/application/gateways
#  pop_fn

  push_fn "Creating ConfigMap \"app-fabric-org1-v1-map\" with Organization 1 information for the application"

cat <<EOF > kubernetes-manifests/configmap/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hyperledger-gql-config
  namespace: test-network
  labels:
    app: hyperledger-gql
data:
  configmap.yaml: |+
    fabric_channel: mychannel
    fabric_contract: token-20
    fabric_wallet_dir: /fabric/application/wallet
    fabric_gateway_hostport: org1-peer1:7051
    fabric_gateway_sslHostOverride: org1-peer1
    fabric_user: appuser_org1
    fabric_gateway_tlsCertPath: /fabric/tlscacerts/org1-tls-ca.pem
    logLevel: debug
    hfcLogging: '{ "debug": "console" }'
    retryDelay: 3000
    maxRetryCount: 5
    hlfCommitTimeout: 3000
    hlfEndorseTimeout: 30
    org1APIKey: 97834158-3224-4CE7-95F9-A148C886653E
    org2APIKey: BC42E734-062D-4AEE-A591-5973CB763430
    asLocalHost: true
    hlfChaincodeName: token-20
    channelName: mychannel
EOF

cat <<EOF > extra/app-fabric-org1-v1-local-map.yaml
fabric_channel: mychannel
fabric_contract: token-20
fabric_wallet_dir: /fabric/application/wallet
fabric_gateway_hostport: org1-peer1:7051
fabric_gateway_sslHostOverride: org1-peer1
fabric_user: appuser_org1
fabric_gateway_tlsCertPath: /fabric/tlscacerts/org1-tls-ca.pem
logLevel: debug
hfcLogging: '{ "debug": "console" }'
retryDelay: 3000
maxRetryCount: 5
hlfCommitTimeout: 3000
hlfEndorseTimeout: 30
org1APIKey: 97834158-3224-4CE7-95F9-A148C886653E
org2APIKey: BC42E734-062D-4AEE-A591-5973CB763430
asLocalHost: true
hlfChaincodeName: token-20
channelName: mychannel
EOF

#  kubectl -n $NS apply -f build/app-fabric-org1-v1-map.yaml

  # todo: could add the second org here

  pop_fn
}


function application_connection() {

 construct_application_configmap

log
 log "For k8s applications:"
 log "Config Maps created for the application"
 log "To deploy your application updated the image name and issue these commands"
 log ""
 log "kubectl -n $NS apply -f kube/application-deployment.yaml"
 log "kubectl -n $NS rollout status deploy/application-deployment"
 log
 log "For non-k8s applications:"
 log "ConnectionPrfiles are in ${PWD}/build/application/gateways"
 log "Identities are in  ${PWD}/build/application/wallets"
 log
}

application_connection