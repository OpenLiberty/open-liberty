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
 * Class Encryption11ElementProxy
 */
public abstract class Encryption11ElementProxy extends ElementProxy {

    protected Encryption11ElementProxy() {
    }

    /**
     * Constructor Encryption11ElementProxy
     *
     * @param doc
     */
    public Encryption11ElementProxy(Document doc) {
        if (doc == null) {
            throw new RuntimeException("Document is null");
        }
        setDocument(doc);
        setElement(XMLUtils.createElementInEncryption11Space(doc, this.getBaseLocalName()));
        String prefix = ElementProxy.getDefaultPrefix(this.getBaseNamespace());
        if (prefix != null && prefix.length() > 0) {
            getElement().setAttribute("xmlns:" + prefix, this.getBaseNamespace());
        }
    }

    /**
     * Constructor Signature11ElementProxy
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public Encryption11ElementProxy(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);

    }

    /** {@inheritDoc} */
    @Override
    public String getBaseNamespace() {
        return EncryptionConstants.EncryptionSpec11NS;
    }
}
