/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.srt.http;

import javax.servlet.http.MappingMatch;
import javax.servlet.http.ServletMapping;

/**
 * An implementation of the Servlet 4.0 ServletMapping API.
 */
public class HttpServletMapping implements ServletMapping {

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
    public HttpServletMapping(MappingMatch mappingMatch, String matchValue,
                              String pattern, String servletName) {
        this.mappingMatch = mappingMatch;
        this.matchValue = matchValue;
        this.pattern = pattern;
        this.servletName = servletName;
        String matchString = "UNKOWN";
        if (mappingMatch.equals(MappingMatch.CONTEXT_ROOT))
            matchString = "CONTEXT_ROOT";
        else if (mappingMatch.equals(MappingMatch.DEFAULT))
            matchString = "DEFAULT";
        else if (mappingMatch.equals(MappingMatch.EXACT))
            matchString = "EXACT";
        else if (mappingMatch.equals(MappingMatch.EXTENSION))
            matchString = "EXTENSION";
        else if (mappingMatch.equals(MappingMatch.PATH))
            matchString = "PATH";

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
