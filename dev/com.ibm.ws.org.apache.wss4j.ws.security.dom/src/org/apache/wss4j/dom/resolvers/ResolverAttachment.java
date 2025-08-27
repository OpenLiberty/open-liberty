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
package org.apache.wss4j.dom.resolvers;

import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.signature.XMLSignatureByteInput;
import org.apache.xml.security.utils.resolver.ResourceResolverContext;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;

/*
 * Fake Resolver for SwA (SOAP with Attachment)
 */
public class ResolverAttachment extends ResourceResolverSpi {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    @Override
    public XMLSignatureInput engineResolveURI(ResourceResolverContext context) throws ResourceResolverException {
        XMLSignatureInput xmlSignatureInput = new XMLSignatureByteInput(EMPTY_BYTE_ARRAY);
        xmlSignatureInput.setSourceURI(context.uriToResolve);
        return xmlSignatureInput;
    }

    /*
     * http://docs.oasis-open.org/wss-m/wss/v1.1.1/os/wss-SwAProfile-v1.1.1-os.html
     * 5.2 Referencing Attachments
     * For simplicity and interoperability this profile limits WS-Security references
     * to attachments to CID scheme URLs. Attachments referenced from WS-Security signature
     * references or cipher references MUST be referenced using CID scheme URLs.
     */
    @Override
    public boolean engineCanResolveURI(ResourceResolverContext context) {
        if (context.uriToResolve == null) {
            return false;
        }
        return context.uriToResolve.startsWith("cid:");
    }

}
