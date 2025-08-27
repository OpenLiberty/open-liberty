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
package org.apache.wss4j.dom.message;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class implements WS Security header.
 *
 * Setup a Security header with a specified actor and mustunderstand flag.
 *
 * The defaults for actor and mustunderstand are: empty <code>actor</code> and
 * <code>mustunderstand</code> is true.
 */
public class WSSecHeader {
    private String actor;

    private boolean mustunderstand = true;

    private Element securityHeader;

    private final Document doc;

    private String wsuPrefix = WSConstants.WSU_PREFIX;

    /**
     * Constructor.
     * @param doc The Document to use when creating the security header
     */
    public WSSecHeader(Document doc) {
        this(null, doc);
    }

    /**
     * Constructor.
     *
     * @param actor The actor name of the <code>wsse:Security</code> header
     * @param doc The Document to use when creating the security header
     */
    public WSSecHeader(String actor, Document doc) {
        this(actor, true, doc);
    }

    /**
     * Constructor.
     *
     * @param act The actor name of the <code>wsse:Security</code> header
     * @param mu Set <code>mustUnderstand</code> to true or false
     * @param doc The Document to use when creating the security header
     */
    public WSSecHeader(String act, boolean mu, Document doc) {
        actor = act;
        mustunderstand = mu;
        this.doc = doc;
    }

    /**
     * set actor name.
     *
     * @param act The actor name of the <code>wsse:Security</code> header
     */
    public void setActor(String act) {
        actor = act;
    }

    /**
     * Set the <code>mustUnderstand</code> flag for the
     * <code>wsse:Security</code> header.
     *
     * @param mu Set <code>mustUnderstand</code> to true or false
     */
    public void setMustUnderstand(boolean mu) {
        mustunderstand = mu;
    }

    /**
     * Get the security header document of this instance.
     *
     * @return The security header element.
     */
    public Document getSecurityHeaderDoc() {
        return this.doc;
    }

    /**
     * Get the security header element of this instance.
     *
     * @return The security header element.
     */
    public Element getSecurityHeaderElement() {
        return securityHeader;
    }

    public void setSecurityHeaderElement(Element securityHeaderElement) {
        this.securityHeader = securityHeaderElement;
    }

    /**
     * Returns whether the security header is empty
     *
     * @return true if empty or if there is no security header
     *         false if non empty security header
     */
    public boolean isEmpty() throws WSSecurityException {
        if (doc == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The Document of WSSecHeader is null"});
        }
        if (securityHeader == null) {
            securityHeader =
                WSSecurityUtil.findWsseSecurityHeaderBlock(
                    doc, doc.getDocumentElement(), actor, false
                );
        }

        return securityHeader == null || securityHeader.getFirstChild() == null;
    }

    /**
     * Creates a security header and inserts it as child into the SOAP Envelope.
     *
     * Check if a WS Security header block for an actor is already available in
     * the document. If a header block is found return it, otherwise a new
     * wsse:Security header block is created and the attributes set
     *
     * @return A <code>wsse:Security</code> element
     */
    public Element insertSecurityHeader() throws WSSecurityException {
        //
        // If there is already a security header in this instance just return it
        //
        if (securityHeader != null) {
            return securityHeader;
        }

        if (doc == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"The Document of WSSecHeader is null"});
        }

        securityHeader =
            WSSecurityUtil.findWsseSecurityHeaderBlock(
                doc, doc.getDocumentElement(), actor, true
            );

        String soapNamespace = WSSecurityUtil.getSOAPNamespace(doc.getDocumentElement());
        String soapPrefix =
            XMLUtils.setNamespace(
                securityHeader, soapNamespace, WSConstants.DEFAULT_SOAP_PREFIX
            );

        if (actor != null && actor.length() > 0) {
            String actorLocal = WSConstants.ATTR_ACTOR;
            if (WSConstants.URI_SOAP12_ENV.equals(soapNamespace)) {
                actorLocal = WSConstants.ATTR_ROLE;
            }
            securityHeader.setAttributeNS(
                soapNamespace,
                soapPrefix + ":" + actorLocal,
                actor
            );
        }
        if (mustunderstand) {
            String mustUnderstandLocal = "1";
            if (WSConstants.URI_SOAP12_ENV.equals(soapNamespace)) {
                mustUnderstandLocal = "true";
            }
            securityHeader.setAttributeNS(
                soapNamespace,
                soapPrefix + ":" + WSConstants.ATTR_MUST_UNDERSTAND,
                mustUnderstandLocal
            );
        }
        wsuPrefix = XMLUtils.setNamespace(securityHeader, WSConstants.WSU_NS, WSConstants.WSU_PREFIX);

        return securityHeader;
    }

    public void removeSecurityHeader() throws WSSecurityException {
        if (securityHeader == null) {
            if (doc == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                              new Object[] {"The Document of WSSecHeader is null"});
            }

            securityHeader =
                WSSecurityUtil.findWsseSecurityHeaderBlock(
                    doc, doc.getDocumentElement(), actor, false
                );
        }

        if (securityHeader != null) {
            Node parent = securityHeader.getParentNode();
            parent.removeChild(securityHeader);
        }
    }

    public String getWsuPrefix() {
        return wsuPrefix;
    }

}
