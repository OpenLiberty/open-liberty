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
package org.apache.xml.security.encryption.keys.content.derivedKey;

import org.apache.xml.security.encryption.KeyDerivationMethod;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.StringJoiner;


/**
 * Class KeyDerivationMethodImpl is an DOM implementation of the KeyDerivationMethod.
 */
public class KeyDerivationMethodImpl extends Encryption11ElementProxy implements KeyDerivationMethod {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(KeyDerivationMethodImpl.class);

    private KDFParams kdfParams;

    /**
     * Constructor KeyDerivationMethodImpl creates a new KeyDerivationMethodImpl instance.
     *
     * @param doc the Document in which to create the DOM tree
     */
    public KeyDerivationMethodImpl(Document doc) {
        super(doc);
    }

    /**
     * Constructor KeyDerivationMethodImpl from existing XML element.
     *
     * @param element the element to use as source
     * @param baseURI the URI of the resource where the XML instance was stored
     * @throws XMLSecurityException if a parsing error occurs
     */
    public KeyDerivationMethodImpl(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * Sets the <code>Algorithm</code> attribute.
     *
     * @param algorithm URI
     */
    public void setAlgorithm(String algorithm) {
        if (algorithm != null) {
            setLocalIdAttribute(EncryptionConstants._ATT_ALGORITHM, algorithm);
        }
    }

    @Override
    public String getAlgorithm() {
        return getLocalAttribute(EncryptionConstants._ATT_ALGORITHM);
    }


    /**
     * Method returns the KDFParams object  of this KeyDerivationMethod Element.
     * If the KDFParams object is not set/cached, it tries to parse it (and cache it)
     * from the KeyDerivationMethod Element. If the KDFParams cannot be parsed/or the
     * Key derivation function URI is not supported, an XMLSecurityException is thrown.
     *
     * @return the Key derivation function parameters.
     * @throws XMLSecurityException if the KDFParams cannot be created or the KDF URI is not supported.
     */
    @Override
    public KDFParams getKDFParams() throws XMLSecurityException {

        if (kdfParams != null) {
            LOG.debug("Returning cached KDFParams");
            return kdfParams;
        }

        String kdfAlgorithm = getAlgorithm();
        if (EncryptionConstants.ALGO_ID_KEYDERIVATION_CONCATKDF.equals(kdfAlgorithm)) {
            Element concatKDFParamsElement =
                    XMLUtils.selectXenc11Node(getElement().getFirstChild(),
                            EncryptionConstants._TAG_CONCATKDFPARAMS, 0);
            kdfParams = new ConcatKDFParamsImpl(concatKDFParamsElement, getBaseURI());
        } else if (EncryptionConstants.ALGO_ID_KEYDERIVATION_HKDF.equals(kdfAlgorithm)) {
            Element hkdfParamsElement =
                    XMLUtils.selectNode(getElement().getFirstChild(),
                            Constants.XML_DSIG_NS_MORE_21_04,
                            EncryptionConstants._TAG_HKDFPARAMS, 0);
            kdfParams = new HKDFParamsImpl(hkdfParamsElement, Constants.XML_DSIG_NS_MORE_07_05);
        } else {
            throw new XMLSecurityException("KeyDerivation.NotSupportedParameter",  new Object[] {kdfAlgorithm});
        }
        return kdfParams;
    }

    public void setKDFParams(KDFParams kdfParams) {
        this.kdfParams = kdfParams;
        if (kdfParams instanceof ElementProxy) {
            appendSelf((ElementProxy)kdfParams);
            addReturnToSelf();
        } else {
            LOG.debug("Could not append KDFParams because it does not implement ElementProxy");
        }
    }


    @Override
    public String getBaseLocalName() {
        return EncryptionConstants._TAG_KEYDERIVATIONMETHOD;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KeyDerivationMethodImpl.class.getSimpleName() + "[", "]")
                .add("kdfParams=" + kdfParams)
                .toString();
    }
}
