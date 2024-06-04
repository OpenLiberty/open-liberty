/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.srt;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.util.RequestUtils;
import com.ibm.wsspi.webcontainer.util.WSServletInputStream;

import io.openliberty.webcontainer60.srt.SRTServletRequest60;
import io.openliberty.webcontainer61.osgi.srt.SRTConnectionContext61;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

public class SRTServletRequest61 extends SRTServletRequest60 implements HttpServletRequest {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer61.srt");
    private static final String CLASS_NAME = SRTServletRequest61.class.getName();

    public SRTServletRequest61(SRTConnectionContext61 context) {
        super(context);
    }

    @Override
    public void initForNextRequest(IRequest req) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "initForNextRequest", "this->" + this + " : " + " req ->" + req);
        }

        super.initForNextRequest(req);
    }

    @Override
    protected WSServletInputStream createInputStream() {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "createInputStream", "this [" + this + "]");
        }

        return new SRTInputStream61(this);
    }

    /*
     * <p>
     * The provided {@code path} parameter is canonicalized as per <a href=
     * "https://jakarta.ee/specifications/servlet/6.0/jakarta-servlet-spec-6.0.html#uri-path-canonicalization">Servlet 6.0,
     * 3.5.2</a> before being used to match resources.
     *
     * @see jakarta.servlet.ServletRequest#getRequestDispatcher​(String)
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST)) {
            logger.entering(CLASS_NAME, "getRequestDispatcher", " path [" + path + "] " + this);
        }

        if (path == null) {
            return null;
        } else {
            path = RequestUtils.normalizePath(path);
            if (path == null)
                return null;

            return super.getRequestDispatcher(path);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletRequest#setCharacterEncoding(java.nio.charset.Charset)
     */
    @Override
    public void setCharacterEncoding(Charset charset) {
        String encoding = (charset == null ? null : charset.name());
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "setCharacterEncoding", "Charset name [" + encoding + "] [" + this + "]");
        }

        try {
            if (encoding != null) {
                super.setCharacterEncoding(encoding, true);
            } else {
                String msg = nls.getFormattedMessage("unsupported.request.encoding.[{0}]", new Object[] { charset }, "Unsupported encoding specified --> " + charset);
                logger.logp(Level.SEVERE, CLASS_NAME, "setRequestCharacterEncoding", msg);
            }
        } catch (UnsupportedEncodingException e) {
            logger.logp(Level.INFO, CLASS_NAME, "setCharacterEncoding", "Unable to set request character encoding based upon Charset", e);
        }
    }
}
