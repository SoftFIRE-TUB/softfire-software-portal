#!/bin/bash

if [ $# == 2 ]; then
	export ip=$1
	shift
else
	export ip=marketplace.openbaton.org
fi

set -e

# export pwd=openbaton

token=`curl -q -u openbatonOSClient:secret -X POST http://$ip:8082/oauth/token -H "Accept:application/json" -d "username=admin&password=$pwd&grant_type=password" | python -c "import sys, json; print json.load(sys.stdin)['access_token']"`

echo ------
echo "$token"


curl POST -F image=@$1 -v -H "Authorization: Bearer $token" http://$ip:8082/api/v1/images

