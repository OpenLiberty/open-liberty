#*******************************************************************************
# Copyright (c) 2024 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-2.0/
# 
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************
#!/usr/bin/env bash

# Ensure that a Node 16 or later LTS environment is used
node_version=`node -v | grep "^v"`
version_number=${node_version:1:2}
if [[ version_number -lt 16 ]]; then
    echo "A Node 16 or later LTS environment must be used when running this script."
    exit 1
else
    if ! [ $((version_number % 2)) -eq 0 ]; then
        echo "A Node 16 or later LTS environment must be used when running this script."
        exit 1        
    fi
fi

# Check version of npm is 8 or higher
npm=`npm -v | sed 's/^[^0-9]*\([0-9]*\).*/\1/'`
if [[ "${npm%.*}" -lt 8 ]]; then
    echo "A version of npm 8 or higher must be used when running this script."
fi

# Check script is run from com.ibm.ws.openapi.ui directory
if ! pwd | grep -q -E 'open-liberty/dev/com.ibm.ws.openapi.ui$' ; then
    echo "This script must be executed from the open-liberty/dev/com.ibm.ws.openapi.ui directory."
    exit 1
fi

# Checking script dependencies
if ! which jq >/dev/null; then
    echo "Please install jq to run this script."
    exit 1
fi

# Update OpenAPI UI dependencies
cd swagger-ui
npx --yes npm-check-updates -u
npm update

# Find new Swagger UI version
export SWAGGER_UI_VERSION=$(jq -r '.packages."node_modules/swagger-ui".version' ./package-lock.json)
echo "Swagger UI version: $SWAGGER_UI_VERSION"

# Copy select files from Swagger UI source
rm -fr swagger-ui-src
git clone --depth 1 -b v${SWAGGER_UI_VERSION} -c advice.detachedHead=false https://github.com/swagger-api/swagger-ui.git swagger-ui-src
rm -r src/original
mkdir src/original
cd swagger-ui-src/src
# Copy all scss files, so we can rebuild with changed colours
find . -iname *.scss -printf %P\\n | xargs cp -t ../../src/original --parents
# Copy URL sanitization utilities
cp core/utils/url.js ../../src/original --parents
cd ../..
rm -rf swagger-ui-src

# Build OpenAPI UI
npm run build -- --mode=production
