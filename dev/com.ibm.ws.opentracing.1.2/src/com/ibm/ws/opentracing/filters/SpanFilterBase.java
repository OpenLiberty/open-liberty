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
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Base class for filters that does the matching on the pattern and other helper methods.
 */
public abstract class SpanFilterBase implements SpanFilter {

    private static final TraceComponent tc = Tr.register(SpanFilterBase.class);

    protected final String pattern;
    protected final SpanFilterType type;
    protected final boolean ignoreCase;
    protected final boolean compareRelative;
    protected final boolean wildcard;
    protected final boolean regex;
    protected final Pattern matcher;

    /**
     * Create a new filter with a pattern, filter type, and whether to ignore case on the pattern match.
     *
     * @param pattern The pattern for which this filter matches.
     * @param type The filter types for which this filter matches.
     * @param ignoreCase Whether to ignore case on the pattern match.
     * @param regex If true, pattern is treated as a regular expression.
     */
    public SpanFilterBase(String pattern, SpanFilterType type, boolean ignoreCase, boolean regex) {

        this.type = type;
        this.ignoreCase = ignoreCase;
        this.regex = regex;

        if (this.regex) {
            this.matcher = Pattern.compile(pattern, this.ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
        } else {
            this.matcher = null;
        }

        String processedPattern = pattern.trim();
        if (ignoreCase) {
            processedPattern = processedPattern.toLowerCase();
        }

        this.wildcard = processedPattern.endsWith("*");

        if (wildcard) {
            processedPattern = processedPattern.substring(0, processedPattern.length() - 2);
        }

        this.pattern = processedPattern;

        this.compareRelative = !pattern.startsWith("http://") && !pattern.startsWith("https://");

        validatePattern();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public String getPattern() {
        return pattern;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public SpanFilterType getType() {
        return type;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String toString() {
        return super.toString() + " { pattern: \"" + pattern + "\", type: \"" + type + "\", regex: \"" + this.matcher + "\" }";
    }

    /**
     * Throw exceptions if the pattern is invalid.
     *
     * @throws IllegalArgumentException Pattern is blank or non-conformant to RFC 2396.
     */
    protected void validatePattern() {
        if (pattern.length() == 0) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "OPENTRACING_FILTER_PATTERN_BLANK"));
        }

        if (!regex) {
            try {
                URI.create(pattern);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "OPENTRACING_FILTER_PATTERN_INVALID", pattern), e);
            }
        }
    }

    /**
     * Check if the URI matches the pattern.
     *
     * @param uri The URI to check against our pattern.
     * @return Whether or not the URI matches our pattern.
     */
    protected boolean match(final URI uri) {

        final String methodName = "match";
        final String uriStr = prepareUri(uri);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, this, uriStr, ignoreCase, wildcard);
        }

        if (this.matcher == null) {
            if (ignoreCase) {
                if (pattern.equalsIgnoreCase(uriStr)) {
                    return true;
                }
                if (wildcard && uriStr.toLowerCase().startsWith(pattern)) {
                    return true;
                }
            } else {
                if (pattern.equals(uriStr)) {
                    return true;
                }
                if (wildcard && uriStr.startsWith(pattern)) {
                    return true;
                }
            }
        } else {
            return this.matcher.matcher(uriStr).matches();
        }

        return false;
    }

    /**
     * Prepare the URI for matching.
     *
     * @param uri The URI.
     * @return A String version of the URI to compare to the pattern.
     */
    protected final String prepareUri(final URI uri) {
        if (compareRelative) {
            final String path = uri.getRawPath();
            final String query = uri.getRawQuery();
            final String fragment = uri.getRawFragment();
            if (query != null && fragment != null) {
                return path + "?" + query + "#" + fragment;
            } else if (query != null) {
                return path + "?" + query;
            } else if (fragment != null) {
                return path + "#" + fragment;
            } else {
                return path;
            }
        } else {
            return uri.toString();
        }
    }

    /**
     * Check if the request context type matches this filter's context type(s).
     *
     * @param contextType The request context type.
     * @return True if this filter matches the context type.
     */
    protected final boolean contextApplies(final SpanFilterType contextType) {
        return (this.type.getValue() & contextType.getValue()) != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean process(final boolean previousState, final URI uri, final SpanFilterType contextType) {

        if (contextApplies(contextType) && match(uri)) {
            return matchAction(previousState, uri, contextType);
        }

        return previousState;
    }

    /**
     * This method will be called when the URI matches this filter's pattern and the type of the request matches this filter's type. Return true to include the request or false to
     * exclude the request (contingent on subsequent filters).
     *
     * @param previousState It is expected that filters are called in sequence, so this is the result of the previous filter, or a default.
     * @param uri The full URI of this request.
     * @param contextType The type of request.
     * @return True to include or false to exclude.
     */
    protected abstract boolean matchAction(final boolean previousState, final URI uri, final SpanFilterType contextType);
}
