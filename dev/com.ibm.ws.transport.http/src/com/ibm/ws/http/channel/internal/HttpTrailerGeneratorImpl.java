/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.HttpTrailerGenerator;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

/**
 *
 */
public class HttpTrailerGeneratorImpl implements HttpTrailerGenerator {

    /** Standard trace registration. */
    private static final TraceComponent tc = Tr.register(HttpTrailerGeneratorImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    HeaderKeys _key;
    String _value;

    public HttpTrailerGeneratorImpl(HeaderKeys key, String value) {
        _key = key;
        _value = value;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.channel.HttpTrailerGenerator#generateTrailerValue(com.ibm.wsspi.genericbnf.HeaderKeys, com.ibm.wsspi.http.channel.HttpTrailers)
     */
    @Override
    public byte[] generateTrailerValue(HeaderKeys hdr, HttpTrailers message) {
        if (hdr.equals(_key)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "generateTrailerValue(HeaderKeys,HttpTrailers): hdr = " + hdr + ", value = " + _value);
            }
            return _value.getBytes();
        } else if (tc.isDebugEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "generateTrailerValue(HeaderKeys,HttpTrailers): header names don't match. requested = " + hdr + ", this object = " + _key);
            }

        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.channel.HttpTrailerGenerator#generateTrailerValue(java.lang.String, com.ibm.wsspi.http.channel.HttpTrailers)
     */
    @Override
    public byte[] generateTrailerValue(String hdr, HttpTrailers message) {

        HeaderKeys key = HttpHeaderKeys.find(hdr);

        if (key != null && hdr.equals(_key)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "generateTrailerValue(String,HttpTrailers): hdr = " + hdr + ", value = " + _value);
            }
            return _value.getBytes();
        } else if (tc.isDebugEnabled()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "generateTrailerValue(HeaderKeys,HttpTrailers): header names don't match. requested = " + hdr + ", this object = " + _key);
            }

        }
        return null;
    }

}
