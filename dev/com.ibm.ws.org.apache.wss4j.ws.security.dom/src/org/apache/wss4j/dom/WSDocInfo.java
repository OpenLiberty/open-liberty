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

package org.apache.wss4j.dom;

/**
 * WSDocInfo holds information about the document to process. It provides a
 * method to store and access document information about BinarySecurityToken,
 * used Crypto, and others.
 *
 * Using the Document's hash a caller can identify a document and get
 * the stored information that may be necessary to process the document.
 * The main usage for this is (are) the transformation functions that
 * are called during Signature/Verification process.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dom.DOMCryptoContext;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.callback.CallbackLookup;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class WSDocInfo {
    private Document doc;
    private Crypto crypto;

    // Here we map the token "Id" to the token itself. The token "Id" is the key as it must be unique to guard
    // against various wrapping attacks. The "Id" name/namespace is stored as part of the entry (along with the
    // element), so that we know what namespace to use when setting the token on the crypto context for signature
    // creation or validation
    private final Map<String, TokenValue> tokens = new HashMap<>();

    private final List<WSSecurityEngineResult> results = new LinkedList<>();
    private final Map<Integer, List<WSSecurityEngineResult>> actionResults = new HashMap<>();
    private CallbackLookup callbackLookup;
    private Element securityHeader;

    public WSDocInfo(Document doc) {
        //
        // This is a bit of a hack. When the Document is a SAAJ SOAPPart instance, it may
        // be that the "owner" document of any child elements is an internal Document, rather
        // than the SOAPPart. This is the case for the SUN SAAJ implementation.
        //
        if (doc != null && doc.getDocumentElement() != null) {
            this.doc = doc.getDocumentElement().getOwnerDocument();
        } else {
            this.doc = doc;
        }
    }

    /**
     * Clears the data stored in this object
     */
    public void clear() {
        crypto = null;
        doc = null;
        callbackLookup = null;
        securityHeader = null;
        tokens.clear();
        results.clear();
        actionResults.clear();
    }

    /**
     * Store a token element for later retrieval. Before storing the token, we check for a
     * previously processed token with the same (wsu/SAML) Id.
     * @param element is the token element to store
     */
    public void addTokenElement(Element element) throws WSSecurityException {
        addTokenElement(element, true);
    }

    /**
     * Store a token element for later retrieval. Before storing the token, we check for a
     * previously processed token with the same (wsu/SAML) Id.
     * @param element is the token element to store
     * @param checkMultipleElements check for a previously stored element with the same Id.
     */
    public void addTokenElement(Element element, boolean checkMultipleElements) throws WSSecurityException {
        if (element == null) {
            return;
        }

        if (element.hasAttributeNS(WSConstants.WSU_NS, "Id")) {
            String id = element.getAttributeNS(WSConstants.WSU_NS, "Id");
            TokenValue tokenValue = new TokenValue("Id", WSConstants.WSU_NS, element);
            TokenValue previousValue = tokens.put(id, tokenValue);
            if (checkMultipleElements && previousValue != null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
                );
            }
        }

        if (element.hasAttributeNS(null, "Id")) {
            String id = element.getAttributeNS(null, "Id");
            TokenValue tokenValue = new TokenValue("Id", null, element);
            TokenValue previousValue = tokens.put(id, tokenValue);
            if (checkMultipleElements && previousValue != null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
                );
            }
        }

        // SAML Assertions
        if ("Assertion".equals(element.getLocalName())) {
            if (WSConstants.SAML_NS.equals(element.getNamespaceURI())
                && element.hasAttributeNS(null, "AssertionID")) {
                String id = element.getAttributeNS(null, "AssertionID");
                TokenValue tokenValue = new TokenValue("AssertionID", null, element);
                TokenValue previousValue = tokens.put(id, tokenValue);
                if (checkMultipleElements && previousValue != null) {
                    throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
                    );
                }
            } else if (WSConstants.SAML2_NS.equals(element.getNamespaceURI())
                && element.hasAttributeNS(null, "ID")) {
                String id = element.getAttributeNS(null, "ID");
                TokenValue tokenValue = new TokenValue("ID", null, element);
                TokenValue previousValue = tokens.put(id, tokenValue);
                if (checkMultipleElements && previousValue != null) {
                    throw new WSSecurityException(
                        WSSecurityException.ErrorCode.INVALID_SECURITY_TOKEN, "duplicateError"
                    );
                }
            }
        }

    }


    /**
     * Get a token Element for the given Id. The Id can be either a wsu:Id or a
     * SAML AssertionID/ID.
     * @param uri is the (relative) uri of the id
     * @return the token element or null if nothing found
     */
    public Element getTokenElement(String uri) {
        String id = XMLUtils.getIDFromReference(uri);
        if (id == null) {
            return null;
        }

        TokenValue token = tokens.get(id);
        if (token != null) {
            return token.getToken();
        }

        return null;
    }

    /**
     * Set all stored tokens on the DOMCryptoContext argument
     * @param context
     */
    public void setTokensOnContext(DOMCryptoContext context) {
        if (!tokens.isEmpty() && context != null) {
            for (Map.Entry<String, TokenValue> entry : tokens.entrySet()) {
                TokenValue tokenValue = entry.getValue();
                context.setIdAttributeNS(tokenValue.getToken(), tokenValue.getIdNamespace(),
                                         tokenValue.getIdName());
            }
        }
    }

    public void setTokenOnContext(String uri, DOMCryptoContext context) {
        String id = XMLUtils.getIDFromReference(uri);
        if (id == null || context == null) {
            return;
        }

        TokenValue tokenValue = tokens.get(id);
        if (tokenValue != null) {
            context.setIdAttributeNS(tokenValue.getToken(), tokenValue.getIdNamespace(),
                                     tokenValue.getIdName());
        }
    }


    /**
     * Store a WSSecurityEngineResult for later retrieval.
     * @param result is the WSSecurityEngineResult to store
     */
    public void addResult(WSSecurityEngineResult result) {
        results.add(result);
        Integer resultTag = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
        if (resultTag != null) {
            List<WSSecurityEngineResult> storedResults = actionResults.get(resultTag);
            if (storedResults == null) {
                storedResults = new ArrayList<>();
            }
            storedResults.add(result);
            actionResults.put(resultTag, storedResults);
        }
    }

    /**
     * Get a copy of the security results list. Modifying the subsequent list does not
     * change the internal results list.
     */
    public List<WSSecurityEngineResult> getResults() {
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(results);
    }

    /**
     * Return a copy of the map between security actions + results. Modifying the subsequent
     * map does not change the internal map.
     */
    public Map<Integer, List<WSSecurityEngineResult>> getActionResults() {
        if (actionResults.isEmpty()) {
            return Collections.emptyMap();
        }
        return new HashMap<>(actionResults);
    }

    /**
     * Get a WSSecurityEngineResult for the given Id.
     * @param uri is the (relative) uri of the id
     * @return the WSSecurityEngineResult or null if nothing found
     */
    public WSSecurityEngineResult getResult(String uri) {
        String id = XMLUtils.getIDFromReference(uri);
        if (id != null && !results.isEmpty()) {
            for (WSSecurityEngineResult result : results) {
                String cId = (String)result.get(WSSecurityEngineResult.TAG_ID);
                if (id.equals(cId)) {
                    return result;
                }
            }
        }
        return null;    //NOPMD
    }

    /**
     * Get a unmodifiable list of WSSecurityEngineResults of the given Integer tag
     */
    public List<WSSecurityEngineResult> getResultsByTag(Integer tag) {
        if (actionResults.isEmpty() || !actionResults.containsKey(tag)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(actionResults.get(tag));
    }

    /**
     * See whether we have a WSSecurityEngineResult of the given Integer tag for the given Id
     */
    public boolean hasResult(Integer tag, String uri) {
        String id = XMLUtils.getIDFromReference(uri);
        if (id == null || uri == null || uri.length() == 0) {
            return false;
        }

        if (!actionResults.isEmpty() && actionResults.containsKey(tag)) {
            for (WSSecurityEngineResult result : actionResults.get(tag)) {
                String cId = (String)result.get(WSSecurityEngineResult.TAG_ID);
                if (id.equals(cId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return the signature crypto class used to process
     *         the signature/verify
     */
    public Crypto getCrypto() {
        return crypto;
    }

    /**
     * @return the document
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * @param crypto is the signature crypto class used to
     *               process signature/verify
     */
    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    /**
     * @param callbackLookup The CallbackLookup object to retrieve elements
     */
    public void setCallbackLookup(CallbackLookup callbackLookup) {
        this.callbackLookup = callbackLookup;
    }

    /**
     * @return the CallbackLookup object to retrieve elements
     */
    public CallbackLookup getCallbackLookup() {
        return callbackLookup;
    }

    /**
     * @return the wsse header being processed
     */
    public Element getSecurityHeader() {
        return securityHeader;
    }

    /**
     * Sets the wsse header being processed
     *
     * @param securityHeader
     */
    public void setSecurityHeader(Element securityHeader) {
        this.securityHeader = securityHeader;
    }

    private static class TokenValue {
        private final String idName;
        private final String idNamespace;
        private final Element token;

        TokenValue(String idName, String idNamespace, Element token) {
            this.idName = idName;
            this.idNamespace = idNamespace;
            this.token = token;
        }

        public String getIdName() {
            return idName;
        }

        public String getIdNamespace() {
            return idNamespace;
        }
        public Element getToken() {
            return token;
        }
    }

}
