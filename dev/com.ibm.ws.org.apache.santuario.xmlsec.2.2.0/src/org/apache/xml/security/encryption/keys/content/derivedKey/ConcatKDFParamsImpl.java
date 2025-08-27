/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.encryption.keys.content.derivedKey;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.StringJoiner;

/**
 * Class ConcatKDFParamsImpl is an DOM representation of the ConcatKDFParams.
 */
public class ConcatKDFParamsImpl extends Encryption11ElementProxy implements KDFParams {


    /**
     * Constructor ConcatKDFParamsImpl creates a new ConcatKDFParamsImpl instance.
     *
     * @param doc the Document in which to create the DOM tree
     */
    public ConcatKDFParamsImpl(Document doc) {
        super(doc);
    }

    /**
     * Constructor ConcatKDFParamsImpl from existing XML element
     * @param element the element to use as source
     * @param baseURI the URI of the resource where the XML instance was stored
     * @throws XMLSecurityException
     */
    public ConcatKDFParamsImpl(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * Sets the <code>Algorithm</code> attribute
     *
     * @param algorithm ID
     */
    public void setAlgorithmId(String algorithm) {
        if (algorithm != null) {
            setLocalAttribute(EncryptionConstants._ATT_ALGORITHM_ID, algorithm);
        }
    }

    public String getAlgorithmId() {
        return getLocalAttribute(EncryptionConstants._ATT_ALGORITHM_ID);
    }

    /**
     * Sets the <code>PartyUInfo</code> attribute
     *
     * @param partyUInfo
     */
    public void setPartyUInfo(String partyUInfo) {
        if (partyUInfo != null) {
            setLocalAttribute(EncryptionConstants._ATT_PARTYUINFO, partyUInfo);
        }
    }

    public String getPartyUInfo() {
        return getLocalAttribute(EncryptionConstants._ATT_PARTYUINFO);
    }

    /**
     * Sets the <code>PartyVInfo</code> attribute
     *
     * @param partyVInfo
     */
    public void setPartyVInfo(String partyVInfo) {
        if (partyVInfo != null) {
            setLocalAttribute(EncryptionConstants._ATT_PARTYVINFO, partyVInfo);
        }
    }

    public String getPartyVInfo() {
        return getLocalAttribute(EncryptionConstants._ATT_PARTYVINFO);
    }

    /**
     * Sets the <code>SuppPubInfo</code> attribute
     *
     * @param suppPubInfo
     */
    public void setSuppPubInfo(String suppPubInfo) {
        if (suppPubInfo != null) {
            setLocalAttribute(EncryptionConstants._ATT_SUPPPUBINFO, suppPubInfo);
        }
    }

    public String getSuppPubInfo() {
        return getLocalAttribute(EncryptionConstants._ATT_SUPPPUBINFO);
    }

    /**
     * Sets the <code>SuppPrivInfo</code> attribute
     *
     * @param suppPrivInfo
     */

    public void setSuppPrivInfo(String suppPrivInfo) {
        if (suppPrivInfo != null) {
            setLocalAttribute(EncryptionConstants._ATT_SUPPPRIVINFO, suppPrivInfo);
        }
    }

    public String getSuppPrivInfo() {
        return getLocalAttribute(EncryptionConstants._ATT_SUPPPRIVINFO);
    }

    public void setDigestMethod(String digestMethod) {
        if (digestMethod != null) {
            Element digestElement =
                    XMLUtils.createElementInSignatureSpace(getDocument(), Constants._TAG_DIGESTMETHOD);
            digestElement.setAttributeNS(null, "Algorithm", digestMethod);
            digestElement.setAttributeNS(
                    Constants.NamespaceSpecNS,
                    "xmlns:" + ElementProxy.getDefaultPrefix(Constants.SignatureSpecNS),
                    Constants.SignatureSpecNS
            );
            appendSelf(digestElement);
        }
    }

    public String getDigestMethod() {
        Element digestElement =
                XMLUtils.selectDsNode(getElement().getFirstChild(), Constants._TAG_DIGESTMETHOD, 0);
        if (digestElement != null) {
            return digestElement.getAttributeNS(null, "Algorithm");
        }
        return null;
    }

    @Override
    public String getBaseLocalName() {
        return EncryptionConstants._TAG_CONCATKDFPARAMS;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConcatKDFParamsImpl.class.getSimpleName() + "[", "]")
                .add("baseURI='" + baseURI + "'")
                .toString();
    }
}
