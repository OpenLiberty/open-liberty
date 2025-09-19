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

package org.apache.wss4j.common.saml;

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.w3c.dom.Text;

/**
 * Override the OpenSAML BASE-64 unmarshaller for X.509 Certificates, to fix a test failure in CXF due to the fact
 * that an X.509 Certificate is only partially unmarshalled.
 *
 * https://issues.apache.org/jira/browse/WSS-695
 */
public final class WSS4JXSBase64BinaryUnmarshaller extends org.opensaml.core.xml.schema.impl.XSBase64BinaryUnmarshaller {

    /**
     * A fix to call Text.getWholeText() instead of Text.getData(), as otherwise with the SAMLRenewTest in CXF's STS
     * systests, the X.509 Certificate is only partially unmarshalled.
     */
    @Override
    protected void unmarshallTextContent(XMLObject xmlObject, Text content) throws UnmarshallingException {
        final String textContent = StringSupport.trimOrNull(content.getWholeText());
        if (textContent != null) {
            processElementContent(xmlObject, textContent);
        }
    }

}
