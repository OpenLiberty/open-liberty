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
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.apache.xml.security.utils.EncryptionConstants;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class HKDFParamsImpl is an DOM representation of the HKDF Parameters.
 */
public class HKDFParamsImpl extends ElementProxy implements KDFParams {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(HKDFParamsImpl.class);

    /**
     * Constructor creates a new HKDFParamsImpl instance.
     *
     * @param doc the Document in which to create the DOM tree
     */
    public HKDFParamsImpl(Document doc) {
        super(doc);
    }

    /**
     * Constructor HKDFParamsImpl from existing XML element
     *
     * @param element the element to use as source
     * @param baseURI the URI of the resource where the XML instance was stored
     * @throws XMLSecurityException if the construction fails for any reason
     */
    public HKDFParamsImpl(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * Sets the <code>DigestMethod</code> Element
     *
     * @param hmacHashAlgorithm is the digest method URI value.
     */
    public void setPRFAlgorithm(String hmacHashAlgorithm) {
        Element targetElement =
                XMLUtils.selectNode(getElement().getFirstChild(), getBaseNamespace(),  EncryptionConstants._TAG_PRF, 0);

        if (hmacHashAlgorithm == null) {
            LOG.debug("HMAC Hash Method is null!");
            if (targetElement != null) {
                LOG.debug("HMAC Hash Method is null, removing PRF element");
                targetElement.getParentNode().removeChild(targetElement);
            }
            return;
        }

        if (targetElement == null) {
            targetElement = createElementForFamilyLocal(getBaseNamespace(), EncryptionConstants._TAG_PRF);
            appendSelf(targetElement);
        }
        targetElement.setAttributeNS(null, Constants._ATT_ALGORITHM, hmacHashAlgorithm);
    }

    /**
     * Returns the <code>DigestMethod</code> algorithm value.
     *
     * @return the digest method URI value.
     */
    public String getPRFAlgorithm() {
        Element prfElement =
                XMLUtils.selectXenc11Node(getElement().getFirstChild(), EncryptionConstants._TAG_PRF, 0);
        if (prfElement != null) {
            return prfElement.getAttributeNS(null, Constants._ATT_ALGORITHM);
        }
        return null;
    }

    /**
     * Sets the <code>Info</code> attribute
     *
     * @param info hex encoded string for the info attribute
     */
    public void setInfo(String info) {
        setLocalElementValue(info, EncryptionConstants._TAG_INFO);
    }

    /**
     * Returns the hex encoded <code>Info</code> attribute
     *
     * @return the info attribute value.
     */
    public String getInfo() {
        return getLocalElementValue(EncryptionConstants._TAG_INFO);
    }

    /**
     * Sets the <code>keyLength</code> attribute. If the keyLength value null, the attribute is ignored.
     *
     * @param keyLength length of the derived key in bytes.
     */
    public void setKeyLength(Integer keyLength) {
        setLocalElementValue(keyLength != null ? keyLength.toString() : null, EncryptionConstants._TAG_KEYLENGTH);
    }

    /**
     * Returns the <code>keyLength</code> attribute value.
     *
     * @return the keyLength attribute value.
     */
    public Integer getKeyLength() {
        String keyLengthStr = getLocalElementValue(EncryptionConstants._TAG_KEYLENGTH);
        Integer keyLength = null;
        if (keyLengthStr != null) {
            try {
                keyLength = Integer.parseInt(keyLengthStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid key length: " + keyLengthStr);
            }
        }
        return keyLength;
    }

    /**
     * Sets the <code>Salt</code> Element
     *
     * @param salt is base64 encoded bytearray.
     */
    public void setSalt(String salt) {
        setLocalElementValue(salt, Constants._TAG_SALT);
    }

    /**
     * Returns the <code>Salt</code> Element value.
     *
     * @return the salt value is base64 encoded bytearray value.
     */
    public String getSalt() {
        return getLocalElementValue(Constants._TAG_SALT);
    }

    @Override
    public String getBaseLocalName() {
        return EncryptionConstants._TAG_HKDFPARAMS;
    }

    @Override
    public String getBaseNamespace() {
        return Constants.XML_DSIG_NS_MORE_21_04;
    }

    public void setLocalElementValue(String value, String elementName) {
        Element targetElement =
                XMLUtils.selectNode(getElement().getFirstChild(), getBaseNamespace(), elementName, 0);

        if (value == null) {
            LOG.debug("Element value: [%s] is null!", elementName);
            if (targetElement != null) {
                LOG.debug("Element value: [%s] is null. Remove element!", elementName);
                targetElement.getParentNode().removeChild(targetElement);
            }
            return;
        }
        if (targetElement == null) {
            targetElement = createElementForFamilyLocal(getBaseNamespace(), elementName);
            appendSelf(targetElement);
        }
        targetElement.setTextContent(value);
    }

    public String getLocalElementValue(String elementName) {
        Element targetElement =
                XMLUtils.selectNode(getElement().getFirstChild(), getBaseNamespace(), elementName, 0);

        if (targetElement != null) {
            return XMLUtils.getFullTextChildrenFromNode(targetElement);
        }
        return null;
    }
}
