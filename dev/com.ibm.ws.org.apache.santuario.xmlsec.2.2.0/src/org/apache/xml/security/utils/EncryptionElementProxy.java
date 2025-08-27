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
package org.apache.xml.security.utils;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class EncryptionElementProxy
 *
 */
public abstract class EncryptionElementProxy extends ElementProxy {

    protected EncryptionElementProxy() {
    }

    /**
     * Constructor EncryptionElementProxy
     *
     * @param doc the {@link Document} in which <code>Encryption Element</code> will be placed
     */
    public EncryptionElementProxy(Document doc) {
        if (doc == null) {
            throw new IllegalArgumentException("Document is null");
        }
        setDocument(doc);
        setElement(XMLUtils.createElementInEncryptionSpace(doc, this.getBaseLocalName()));
        String prefix = ElementProxy.getDefaultPrefix(this.getBaseNamespace());
        if (prefix != null && !prefix.isEmpty()) {
            getElement().setAttribute("xmlns:" + prefix, this.getBaseNamespace());
        }
    }

    /**
     * Constructor EncryptionElementProxy
     *
     * @param element <code>Encryption Element</code>
     * @param baseURI the namespace URI of element
     * @throws XMLSecurityException if a {@link XMLSecurityException} occurs
     */
    public EncryptionElementProxy(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /** {@inheritDoc} */
    @Override
    public String getBaseNamespace() {
        return EncryptionConstants.EncryptionSpecNS;
    }
}
