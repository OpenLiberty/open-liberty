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
package org.apache.xml.security.encryption.keys;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.EncryptionConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The RecipientKeyInfo element is used in the <code>ds:KeyInfo</code> with AgreementMethod elements.
 * @see <a href="https://www.w3.org/TR/xmlenc-core1/#sec-Alg-KeyAgreement">KeyAgreement</a>. The RecipientKeyInfo
 * element extends ds:KeyInfo with namespace xenc (http://www.w3.org/2001/04/xmlenc#) and is used to describe
 * the recipient's key.
 */
public class RecipientKeyInfo extends KeyInfoEnc {

    public RecipientKeyInfo(Document doc) {
        super(doc);
    }

    public RecipientKeyInfo(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseNamespace() {
        return EncryptionConstants.EncryptionSpecNS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseLocalName() {
        return EncryptionConstants._TAG_RECIPIENTKEYINFO;
    }
}
