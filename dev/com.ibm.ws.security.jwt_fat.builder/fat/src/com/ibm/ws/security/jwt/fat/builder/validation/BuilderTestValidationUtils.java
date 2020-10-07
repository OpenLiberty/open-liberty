/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import static org.junit.Assert.fail;

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
        Log.info(thisClass, thisMethod, "keys: " + keys);

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
            if (n == null) {
                fail("Size of the signature checking failed as the signature (\"n\" value) was missing");
            }
            Log.info(thisClass, thisMethod, "n: " + n);
            String decoded_n = new String(Base64.decodeBase64(n));
            Log.info(thisClass, thisMethod, "raw size of n: " + decoded_n.length());
            int calculatedSize = decoded_n.length() * 8;
            Log.info(thisClass, thisMethod, "Comparing expected size of signature (" + keySize + ") against size (" + calculatedSize + ") calculated from the token");
            assertTrue("The size of the signature in the token (" + calculatedSize + ") did NOT match the expected size (" + keySize + ")", calculatedSize == keySize);

        }

    }

    public void validateCurve(Page endpointOutput, String curve) throws Exception {

        String thisMethod = "validateCurve";
        loggingUtils.printMethodName(thisMethod, "Start of");

        String jwtToken = WebResponseUtils.getResponseText(endpointOutput);

        Log.info(thisClass, thisMethod, "response value: " + jwtToken);
        JSONObject val = JSONObject.parse(jwtToken);

        String keys = val.get("keys").toString();
        Log.info(thisClass, thisMethod, "keys: " + keys);

        JSONArray keyArrays = (JSONArray) val.get("keys");
        // We are now generating two jwks again by default like before and should get the latest element
        int cnt = 0;
        for (Object o : keyArrays) {
            JSONObject jsonLineItem = (JSONObject) o;
            if (keyArrays.size() > 1 && cnt < 1) {
                cnt++;
                continue;
            }

            String crv = (String) jsonLineItem.get("crv");
            if (crv == null) {
                fail("Curve checking failed as the curve (\"crv\" value) was missing");
            }
            Log.info(thisClass, thisMethod, "crv: " + crv);
            Log.info(thisClass, thisMethod, "Comparing expected crv (" + curve + ") against actual crv (" + crv + ")");
            assertTrue("The actual crv (" + crv + ") did NOT match the expected curve (" + curve + ")", curve.equals(crv));

        }
    }

    public void validateSigAlg(Page endpointOutput, String sigAlg) throws Exception {

        String thisMethod = "validateSigAlg";
        loggingUtils.printMethodName(thisMethod, "Start of");

        String jwtToken = WebResponseUtils.getResponseText(endpointOutput);

        Log.info(thisClass, thisMethod, "response value: " + jwtToken);
        JSONObject val = JSONObject.parse(jwtToken);

        String keys = val.get("keys").toString();
        Log.info(thisClass, thisMethod, "keys: " + keys);

        JSONArray keyArrays = (JSONArray) val.get("keys");
        // We are now generating two jwks again by default like before and should get the latest element
        int cnt = 0;
        for (Object o : keyArrays) {
            JSONObject jsonLineItem = (JSONObject) o;
            if (keyArrays.size() > 1 && cnt < 1) {
                cnt++;
                continue;
            }

            String alg = (String) jsonLineItem.get("alg");
            if (alg == null) {
                fail("Signature Algorithm checking failed as the signature algorithm (\"alg\" value) was missing");
            }
            Log.info(thisClass, thisMethod, "alg: " + alg);
            Log.info(thisClass, thisMethod, "Comparing expected signature algorithm (" + sigAlg + ") against actual alg (" + alg + ")");
            assertTrue("The actual signature algorithm (" + sigAlg + ") did NOT match the expected signature algorithm (" + sigAlg + ")", sigAlg.equals(alg));

        }
    }

}