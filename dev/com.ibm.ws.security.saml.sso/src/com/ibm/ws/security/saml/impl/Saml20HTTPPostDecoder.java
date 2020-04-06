/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.binding.decoding.HTTPPostDecoder;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.ws.transport.http.HTTPInTransport;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.TraceConstants;

/** Message decoder implementing the SAML 2.0 HTTP POST binding. */
public class Saml20HTTPPostDecoder extends HTTPPostDecoder {
    private static TraceComponent tc = Tr.register(Saml20HTTPPostDecoder.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    String acsUrl;

    public Saml20HTTPPostDecoder(String acsUrl) {
        this.acsUrl = acsUrl;
    }

    @Override
    protected String getActualReceiverEndpointURI(@SuppressWarnings("rawtypes") SAMLMessageContext messageContext) throws MessageDecodingException {
        return acsUrl;
    }

    @Override
    protected InputStream getBase64DecodedMessage(HTTPInTransport transport) throws MessageDecodingException {
        InputStream inputStream = super.getBase64DecodedMessage(transport);
        if (tc.isDebugEnabled()) {
            if (inputStream instanceof ByteArrayInputStream) {
                ByteArrayInputStream byteStream = (ByteArrayInputStream) inputStream;
                int iAvailable = byteStream.available();
                if (iAvailable > 0) {
                    byte[] bytes = new byte[iAvailable];
                    byteStream.read(bytes, 0, iAvailable);
                    byteStream.reset();
                    try {
                        Tr.debug(tc, "decoded saml response:" + new String(bytes, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        // ignore it
                        Tr.debug(tc, "decoded saml response failed to converted to a String:" + e);
                    }
                } else {
                    Tr.debug(tc, "decoded saml response has bytes count:" + iAvailable);
                }
            }
        }
        return inputStream;
    }
}