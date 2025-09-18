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

package org.apache.wss4j.dom.message.token;

import java.util.Arrays;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.bsp.BSPRule;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Signature Confirmation element.
 */
public class SignatureConfirmation {

    public static final String SC_VALUE_ATTR = "Value";
    private Element element;
    private byte[] signatureValue;

    /**
     * Constructs a <code>SignatureConfirmation</code> object and parses the
     * <code>wsse11:SignatureConfirmation</code> element to initialize it.
     *
     * @param elem the <code>wsse11:SignatureCOnfirmation</code> element that
     *             contains the confirmation data
     * @param bspEnforcer a BSPEnforcer instance used to enforce BSP rules
     */
    public SignatureConfirmation(Element elem, BSPEnforcer bspEnforcer) throws WSSecurityException {
        element = elem;

        String id = getID();
        if (id == null || id.length() == 0) {
            bspEnforcer.handleBSPRule(BSPRule.R5441);
        }

        String sv = element.getAttributeNS(null, SC_VALUE_ATTR);
        if (sv != null) {
            signatureValue = org.apache.xml.security.utils.XMLUtils.decode(sv);
        }
    }

    /**
     * Constructs a <code>SignatureConfirmation</code> object according
     * to the defined parameters.
     *
     * @param doc the SOAP envelope as <code>Document</code>
     * @param signVal the Signature value as byte[] of <code>null</code>
     * if no value available.
     */
    public SignatureConfirmation(Document doc, byte[] signVal) {
        element =
            doc.createElementNS(
                WSConstants.WSSE11_NS,
                WSConstants.WSSE11_PREFIX + ":"  + WSConstants.SIGNATURE_CONFIRMATION_LN
            );
        XMLUtils.setNamespace(element, WSConstants.WSSE11_NS, WSConstants.WSSE11_PREFIX);
        if (signVal != null) {
            String sv = org.apache.xml.security.utils.XMLUtils.encodeToString(signVal);
            element.setAttributeNS(null, SC_VALUE_ATTR, sv);
        }
    }

    /**
     * Add the WSU Namespace to this SC. The namespace is not added by default for
     * efficiency purposes.
     */
    public void addWSUNamespace() {
        element.setAttributeNS(XMLUtils.XMLNS_NS, "xmlns:" + WSConstants.WSU_PREFIX, WSConstants.WSU_NS);
    }

    /**
     * Returns the dom element of this <code>SignatureConfirmation</code> object.
     *
     * @return the <code>wsse11:SignatureConfirmation</code> element
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the string representation of the token.
     *
     * @return a XML string representation
     */
    public String toString() {
        return DOM2Writer.nodeToString(element);
    }

    /**
     * Set wsu:Id attribute of this SignatureConfirmation element.
     * @param id
     */
    public void setID(String id) {
        element.setAttributeNS(WSConstants.WSU_NS, WSConstants.WSU_PREFIX + ":Id", id);
    }

    /**
     * Returns the value of the wsu:Id attribute
     * @return the WSU ID
     */
    public String getID() {
        return element.getAttributeNS(WSConstants.WSU_NS, "Id");
    }

    /**
     * @return Returns the signatureValue.
     */
    public byte[] getSignatureValue() {
        return signatureValue;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (signatureValue != null) {
            result = 31 * result + Arrays.hashCode(signatureValue);
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SignatureConfirmation)) {
            return false;
        }
        SignatureConfirmation signatureConfirmation = (SignatureConfirmation)object;
        byte[] sigValue = signatureConfirmation.getSignatureValue();
        if (!Arrays.equals(sigValue, getSignatureValue())) {
            return false;
        }
        return true;
    }

}
