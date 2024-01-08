/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.srt;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.response.IResponse;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

import io.openliberty.webcontainer60.srt.SRTServletResponse60;
import io.openliberty.webcontainer61.osgi.srt.SRTConnectionContext61;
import jakarta.servlet.http.HttpServletResponse;

public class SRTServletResponse61 extends SRTServletResponse60 implements HttpServletResponse {
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("io.openliberty.webcontainer61.srt");
    private static final String CLASS_NAME = SRTServletResponse61.class.getName();

    public SRTServletResponse61(SRTConnectionContext61 context) {
        super(context);
    }

    @Override
    public void initForNextResponse(IResponse resp) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "initForNextRequest", "this [" + this + "] , resp [" + resp + "]");
        }

        super.initForNextResponse(resp);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletResponse#setCharacterEncoding(java.nio.charset.Charset)
     */
    @Override
    public void setCharacterEncoding(Charset charset) {
        String encoding = charset.name();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.entering(CLASS_NAME, "setCharacterEncoding", "Charset encoding [" + encoding + "] [" + this + "]");
        }
        super.setCharacterEncoding(encoding);
    }

    @Override
    public void sendRedirect(String s, int status, boolean b) {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "sendRedirect", "this [" + this + "] , to be implemented - sendRedirect(String, int, boolean");
        }
    }
}
