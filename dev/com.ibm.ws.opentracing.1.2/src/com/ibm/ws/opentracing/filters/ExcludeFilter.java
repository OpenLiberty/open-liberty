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
package com.ibm.ws.opentracing.filters;

import java.net.URI;

/**
 * If the URI and type match, then exclude the request.
 */
public class ExcludeFilter extends SpanFilterBase {
    /**
     * Create a new filter with a pattern, filter type, and whether to ignore case on the pattern match.
     * If the URI and type match, then exclude the request.
     *
     * @param pattern The pattern for which this filter matches.
     * @param type The filter types for which this filter matches.
     * @param ignoreCase Whether to ignore case on the pattern match.
     * @param regex If true, pattern is treated as a regular expression.
     */
    public ExcludeFilter(String pattern, SpanFilterType type, boolean ignoreCase, boolean regex) {
        super(pattern, type, ignoreCase, regex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final boolean matchAction(final boolean previousState, final URI uri, final SpanFilterType contextType) {
        return false;
    }
}
