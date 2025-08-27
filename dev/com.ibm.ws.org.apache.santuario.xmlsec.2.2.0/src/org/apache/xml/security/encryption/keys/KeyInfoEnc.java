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
package org.apache.xml.security.encryption.keys;

import org.apache.xml.security.encryption.AgreementMethod;
import org.apache.xml.security.encryption.keys.content.AgreementMethodImpl;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.utils.ElementProxy;
import org.apache.xml.security.utils.EncryptionConstants;
import org.apache.xml.security.utils.I18n;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * This class is the extension of the {@link org.apache.xml.security.keys.KeyInfo} class.
 * The {@link org.apache.xml.security.keys.KeyInfo} implements XML structures defined in XML Signature standards,
 * and this class extends it for handling XML Element types defined by the XML encryption standards,
 * such as AgreementMethod.
 */
public class KeyInfoEnc extends KeyInfo {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(KeyInfoEnc.class);

    /**
     * @see KeyInfo
     */
    public KeyInfoEnc(Document doc) {
        super(doc);
    }

    /**
     * @see KeyInfo
     */
    public KeyInfoEnc(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * Method add AgreementMethod to the KeyInfo
     *
     * @param agreementMethod the AgreementMethod to be added. The AgreementMethod must extend
     *                        class {@link ElementProxy}
     */
    public void add(AgreementMethod agreementMethod) {

        if (agreementMethod instanceof ElementProxy) {
            LOG.debug("Adding agreementMethod with algorithm {0}" + agreementMethod.getAlgorithm());
            appendSelf((ElementProxy) agreementMethod);
            addReturnToSelf();
        } else {
            Object[] exArgs = {EncryptionConstants._TAG_AGREEMENTMETHOD, agreementMethod.getClass().getName()};
            throw new IllegalArgumentException(I18n.translate("KeyValue.IllegalArgument", exArgs));
        }
    }

    /**
     * Method lengthAgreementMethod
     *
     * @return the number of the AgreementMethod tags
     */
    public int lengthAgreementMethod() {
        return this.length(EncryptionConstants.EncryptionSpecNS, EncryptionConstants._TAG_AGREEMENTMETHOD);
    }

    /**
     * Method itemAgreementMethod
     *
     * @param i index of the AgreementMethod element
     * @return the i(th) AgreementMethod proxy element or null if the index is too big
     * @throws XMLSecurityException if the element with AgreementMethod exists but with wrong namespace
     */
    public AgreementMethod itemAgreementMethod(int i) throws XMLSecurityException {
        Element e = XMLUtils.selectXencNode(
                getFirstChild(), EncryptionConstants._TAG_AGREEMENTMETHOD, i);

        if (e == null) {
            LOG.debug("No AgreementMethod element at position [{0}]" + i);
            return null;
        }
        return new AgreementMethodImpl(e);
    }

    /**
     * Method containsAgreementMethod returns true if the KeyInfo contains a AgreementMethod node
     *
     * @return true if the KeyInfo contains a AgreementMethod node else false
     */
    public boolean containsAgreementMethod() {
        return this.lengthAgreementMethod() > 0;
    }
}
