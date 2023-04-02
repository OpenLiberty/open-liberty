/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.srt.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.MappingMatch;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

/**
 * An implementation of the Servlet 4.0 ServletMapping API.
 */
public class HttpServletMappingImpl implements HttpServletMapping {
    private static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer40.srt");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer40.srt.http.HttpServletMappingImpl";

    private final MappingMatch mappingMatch;
    private final String matchValue;
    private final String pattern;
    private final String servletName;

    /**
     *
     * @param mappingMatch
     * @param matchValue
     * @param pattern
     * @param servletName
     */
    public HttpServletMappingImpl(MappingMatch mappingMatch, String matchValue,
                                  String pattern, String servletName) {
        this.mappingMatch = mappingMatch;
        this.matchValue = matchValue;
        this.pattern = pattern;
        this.servletName = servletName;

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "Constructor ", "mappingMatch [" + this.mappingMatch + "] , matchValue [" + this.matchValue + "] , pattern [" + this.pattern
                                                                + "] , servletName [" + this.servletName + "] , this [" + this + "]");
        }
    }

    /** {@inheritDoc} */
    @Override
    public MappingMatch getMappingMatch() {
        return this.mappingMatch;
    }

    /** {@inheritDoc} */
    @Override
    public String getMatchValue() {
        return this.matchValue;
    }

    /** {@inheritDoc} */
    @Override
    public String getPattern() {
        return this.pattern;
    }

    /** {@inheritDoc} */
    @Override
    public String getServletName() {
        return this.servletName;
    }

}
