/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.dom.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.common.SecurityActionToken;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.UsernameTokenUtil;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandler;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;

/**
 * Sign a request using a secret key derived from UsernameToken data.
 *
 * Enhanced by Alberto Coletti to support digest password type for
 * username token signature
 */

public class UsernameTokenSignedAction implements Action {
    public void execute(WSHandler handler, SecurityActionToken actionToken, RequestData reqData)
            throws WSSecurityException {
        CallbackHandler callbackHandler = reqData.getCallbackHandler();
        if (callbackHandler == null) {
            callbackHandler = handler.getPasswordCallbackHandler(reqData);
        }
        WSPasswordCallback passwordCallback =
            handler.getPasswordCB(reqData.getUsername(), WSConstants.UT_SIGN, callbackHandler, reqData);

        if (reqData.getUsername() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noUser");
        }

        WSSecUsernameToken builder = new WSSecUsernameToken(reqData.getSecHeader());
        builder.setIdAllocator(reqData.getWssConfig().getIdAllocator());
        builder.setPrecisionInMilliSeconds(reqData.isPrecisionInMilliSeconds());
        builder.setWsTimeSource(reqData.getWssConfig().getCurrentTime());
        builder.setWsDocInfo(reqData.getWsDocInfo());
        builder.setExpandXopInclude(reqData.isExpandXopInclude());

        int iterations = reqData.getDerivedKeyIterations();
        boolean useMac = reqData.isUseDerivedKeyForMAC();
        builder.addDerivedKey(useMac, iterations);

        builder.setUserInfo(reqData.getUsername(), passwordCallback.getPassword());
        builder.addCreated();
        builder.addNonce();
        byte[] salt = UsernameTokenUtil.generateSalt(useMac);
        builder.prepare(salt);

        // Now prepare to sign.
        // First step:  Get a WS Signature object and set config parameters
        // second step: set user data and algorithm parameters. This
        //              _must_ be done before we "prepare"
        // third step:  Call "prepare". This creates the internal WS Signature
        //              data structures, XML element, fills in the algorithms
        //              and other data.
        // fourth step: Get the references. These references identify the parts
        //              of the document that will be included into the
        //              signature. If no references are given sign the message
        //              body by default.
        // fifth step:  compute the signature
        //
        // after "prepare" the Signature XML element is ready and may prepend
        // this to the security header.

        SignatureActionToken signatureToken = null;
        if (actionToken instanceof SignatureActionToken) {
            signatureToken = (SignatureActionToken)actionToken;
        }
        if (signatureToken == null) {
            signatureToken = reqData.getSignatureToken();
        }

        WSSecSignature sign = new WSSecSignature(reqData.getSecHeader());
        sign.setIdAllocator(reqData.getWssConfig().getIdAllocator());
        sign.setAddInclusivePrefixes(reqData.isAddInclusivePrefixes());

        sign.setCustomTokenValueType(WSConstants.USERNAMETOKEN_NS + "#UsernameToken");
        sign.setCustomTokenId(builder.getId());
        sign.setSecretKey(builder.getDerivedKey(salt));
        sign.setKeyIdentifierType(WSConstants.CUSTOM_SYMM_SIGNING);
        if (signatureToken.getDigestAlgorithm() != null) {
            sign.setDigestAlgo(signatureToken.getDigestAlgorithm());
        }

        if (signatureToken.getSignatureAlgorithm() != null) {
            sign.setSignatureAlgorithm(signatureToken.getSignatureAlgorithm());
        } else {
            sign.setSignatureAlgorithm(WSConstants.HMAC_SHA1);
        }

        sign.prepare(null);

        // prepend in this order: first the Signature Element and then the
        // UsernameToken Element. This way the server gets the UsernameToken
        // first, can check it and are prepared to compute the Signature key.
        // sign.prependToHeader(reqData.getSecHeader());
        // builder.prependToHeader(reqData.getSecHeader());

        List<WSEncryptionPart> parts = null;
        if (!signatureToken.getParts().isEmpty()) {
            parts = signatureToken.getParts();
        } else {
            parts = new ArrayList<>(1);
            Document doc = reqData.getSecHeader().getSecurityHeaderElement().getOwnerDocument();
            parts.add(WSSecurityUtil.getDefaultEncryptionPart(doc));
        }
        List<javax.xml.crypto.dsig.Reference> referenceList = sign.addReferencesToSign(parts);

        try {
            sign.computeSignature(referenceList);
            reqData.getSignatureValues().add(sign.getSignatureValue());
        } catch (WSSecurityException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e,
                    "empty", new Object[] {"WSHandler: Error during UsernameTokenSignature"}
            );
        }
        builder.prependToHeader();

        Arrays.fill(salt, (byte)0);
    }
}
