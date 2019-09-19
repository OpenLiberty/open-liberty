/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.validation;

import static org.junit.Assert.assertTrue;

import org.apache.commons.codec.binary.Base64;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

public class BuilderTestValidationUtils extends TestValidationUtils {

    protected static Class<?> thisClass = BuilderTestValidationUtils.class;

    public void validateSignatureSize(Page endpointOutput, int keySize) throws Exception {

        String thisMethod = "validateSignatureSize";
        loggingUtils.printMethodName(thisMethod, "Start of");

        String jwtToken = WebResponseUtils.getResponseText(endpointOutput);

        Log.info(thisClass, thisMethod, "response value: " + jwtToken);
        JSONObject val = JSONObject.parse(jwtToken);

        String keys = val.get("keys").toString();

        JSONArray keyArrays = (JSONArray) val.get("keys");
        // We are now generating two jwks again by default like before and should get the latest element
        int cnt = 0;
        for (Object o : keyArrays) {
            JSONObject jsonLineItem = (JSONObject) o;
            if (keyArrays.size() > 1 && cnt < 1) {
                cnt++;
                continue;
            }

            String n = (String) jsonLineItem.get("n");
            String decoded_n = new String(Base64.decodeBase64(n));
            Log.info(thisClass, thisMethod, "n: " + n);
            Log.info(thisClass, thisMethod, "raw size of n: " + decoded_n.length());
            int calculatedSize = decoded_n.length() * 8;
            Log.info(thisClass, thisMethod, "Comparing expected size of signature (" + keySize + ") against size (" + calculatedSize + ") calculated from the token");
            assertTrue("The size of the signature in the token (" + calculatedSize + ") did NOT match the expected size (" + keySize + ")", calculatedSize == keySize);

        }

    }

}