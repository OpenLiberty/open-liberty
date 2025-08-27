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

package org.apache.wss4j.dom.processor;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.w3c.dom.Element;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;

import java.util.List;
import java.io.IOException;

/**
 * The processor to process <code>wsc:SecurityContextToken</code>.
 */
public class SecurityContextTokenProcessor implements Processor {

    public List<WSSecurityEngineResult> handleToken(
        Element elem,
        RequestData data
    ) throws WSSecurityException {
        SecurityContextToken sct = new SecurityContextToken(elem);

        Validator validator =
            data.getValidator(new QName(elem.getNamespaceURI(), elem.getLocalName()));

        WSSecurityEngineResult result =
            new WSSecurityEngineResult(WSConstants.SCT, sct);
        if (validator != null) {
            // Hook to allow the user to validate the SecurityContextToken
            Credential credential = new Credential();
            credential.setSecurityContextToken(sct);

            Credential returnedCredential = validator.validate(credential, data);
            result.put(WSSecurityEngineResult.TAG_VALIDATED_TOKEN, Boolean.TRUE);
            String tokenId = sct.getID();
            if (tokenId.length() != 0) {
                result.put(WSSecurityEngineResult.TAG_ID, tokenId);
            }
            result.put(WSSecurityEngineResult.TAG_SECRET, returnedCredential.getSecretKey());
        } else {
            String id = sct.getID();
            id = XMLUtils.getIDFromReference(id);

            byte[] secret = null;
            try {
                secret = getSecret(data.getCallbackHandler(), sct.getIdentifier());
            } catch (WSSecurityException ex) {
                secret = getSecret(data.getCallbackHandler(), id);
            }
            if (secret == null || secret.length == 0) {
                secret = getSecret(data.getCallbackHandler(), id);
            }
            result.put(WSSecurityEngineResult.TAG_ID, sct.getID());
            result.put(WSSecurityEngineResult.TAG_SECRET, secret);
        }

        data.getWsDocInfo().addTokenElement(elem);
        data.getWsDocInfo().addResult(result);
        return java.util.Collections.singletonList(result);
    }

    /**
     * Get the secret from the provided callback handler and return it.
     *
     * @param cb
     * @param identifier
     * @return The key collected using the callback handler
     */
    private byte[] getSecret(CallbackHandler cb, String identifier)
        throws WSSecurityException {

        if (cb == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCallback");
        }

        WSPasswordCallback callback =
            new WSPasswordCallback(identifier, WSPasswordCallback.SECURITY_CONTEXT_TOKEN);
        try {
            Callback[] callbacks = new Callback[]{callback};
            cb.handle(callbacks);
        } catch (IOException | UnsupportedCallbackException e) {
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILURE, e,
                "noKey",
                new Object[] {identifier});
        }

        return callback.getKey();
    }

}
