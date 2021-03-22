/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.saml2.binding.decoding.impl.HTTPPostDecoder;


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

    // Override
    protected String getActualReceiverEndpointURI(@SuppressWarnings("rawtypes") MessageContext messageContext) throws MessageDecodingException {
        return acsUrl;
    }

    @Override
    protected InputStream getBase64DecodedMessage(HttpServletRequest request) throws MessageDecodingException {
        InputStream inputStream = super.getBase64DecodedMessage(request);
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