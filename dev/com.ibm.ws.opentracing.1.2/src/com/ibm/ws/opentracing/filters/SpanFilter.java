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
 * There are two types of configuration elements: excludeSpans and includeSpans.
 *
 * <code>
 * &lt;opentracing&gt;
 * &lt;excludeSpans pattern="URI" type="INCOMING|OUTGOING|BOTH" ignoreCase="true|false" regex="true|false" /&gt;
 * &lt;includeSpans pattern="URI" type="INCOMING|OUTGOING|BOTH" ignoreCase="true|false" regex="true|false" /&gt;
 * &lt;/opentracing&gt;
 * </code>
 *
 * These are called filters. Each filter may be specified 0 or more times.
 *
 * When a request comes in to the container, all INCOMING or BOTH filters are checked with
 * the incoming URI. If all checks return true, then a new span is created for the incoming
 * request; otherwise, no new span is created, but any incoming spans are passed through.
 *
 * When a request is sent from the container, all OUTGOING or BOTH filters are checked with
 * the outgoing URI. If all checks return true, then a new span is created for the outgoing
 * request; otherwise, no new span is created, but any incoming spans are passed through.
 *
 * The default type of a filter is BOTH.
 *
 * All filters have a pattern attribute which is the URI pattern to match the filter. If
 * the pattern starts with http(s)://, then the filter will compare to the absolute URI;
 * otherwise, the filter will compare to the URI starting with the path. In both cases, the
 * filter will compare with query and fragment, if available, for both the pattern and the
 * URI.
 *
 * If regex is false (the default) and pattern ends with '*', then the filter will check if
 * the URI starts with the pattern; otherwise, the filter will do an exact match. In both
 * cases, the comparison will be done in a case sensitive or insensitive way depending on
 * the value of ignoreCase. The default of ignoreCase is true.
 *
 * If regex is true, pattern is compiled into a {@see java.util.regex.Pattern} and
 * the URI is matched interpreting the pattern as a regular expression. The comparison will
 * be done in a case sensitive or insensitive way depending on the value of ignoreCase.
 * Regular expressions are discouraged because they are resource intensive. One common
 * mistake when using regular expressions is that some URL components such as the query
 * separator (?) have special meaning in regular expressions and must be escaped with '\'.
 *
 * If no filters match the URI, then the default is that the span is created.
 *
 * The filters are processed in the order in which they appear in the configuration. For
 * example, if we want to only process URIs that start with /api, then we can use an
 * excludeSpans first with a pattern of '*' to exclude all URIs (this is needed since,
 * by default, spans are created), and then we can use an includeSpans with a pattern of
 * '/api*'.
 *
 * Both the pattern and URI are compared with RFC 2396-compliant % URI-encoding, when
 * needed.
 */
public interface SpanFilter {

    /**
     * Bit flag representing an incoming request.
     */
    public static final int SPAN_TYPE_INCOMING = 1 << 0;

    /**
     * Bit flag representing an outgoing request.
     */
    public static final int SPAN_TYPE_OUTGOING = 1 << 1;

    /**
     * Returns the pattern for this filter.
     *
     * @return Pattern for this filter.
     */
    String getPattern();

    /**
     * Returns the type(s) this filter supports.
     *
     * @return type(s) this filter supports.
     */
    SpanFilterType getType();

    /**
     * Process a filter in sequence and return true if a span should be created for this request.
     *
     * @param previousState It is expected that filters are called in sequence, so this is the result of the previous filter, or a default.
     * @param uri The full URI of this request.
     * @param contextType The type of request.
     * @return True if a span should be created for this request; otherwise, false.
     */
    boolean process(final boolean previousState, final URI uri, final SpanFilterType contextType);
}
