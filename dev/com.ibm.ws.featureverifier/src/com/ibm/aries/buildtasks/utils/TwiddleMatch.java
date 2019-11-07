/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.utils;

import java.util.regex.Pattern;

/**
 * A TwiddleMatch is constructed from a string and is used to test if other
 * strings match it. If the TwiddleMatch is constructed from a string that does
 * NOT start with a twiddle (~), the other string must match exactly. If the
 * twiddle match is constructed from a string that DOES start with a twiddle, the
 * twiddle is removed and the rest is treated as a regular expression (i.e. the other
 * strings are tested against that regular expression).
 * 
 */
public final class TwiddleMatch
{
    private final String _spec;
    private final String _exact;
    private final Pattern _pattern;

    /* -------------------------------------------------------------------------- */
    /*
     * TwiddleMatch constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new TwiddleMatch that matches nothing (not even null)
     */
    public TwiddleMatch()
    {
        this("");
    }

    /* -------------------------------------------------------------------------- */
    /*
     * TwiddleMatch constructor
     * /* --------------------------------------------------------------------------
     */
    /**
     * Construct a new TwiddleMatch.
     * 
     * @param matchSpec The specification of the match (starts with a ~ means it's a regular expression,
     *            otherwise it only matches the exact string (except for
     *            null, which means nothing (not even null) matches)
     */
    public TwiddleMatch(String matchSpec)
    {
        _spec = matchSpec;
        if (matchSpec != null)
        {
            if (matchSpec.startsWith("~"))
            {
                _pattern = Pattern.compile(matchSpec.substring(1));
                _exact = null;
            }
            else
            {
                _pattern = null;
                _exact = matchSpec;
            }
        }
        else
        {
            _pattern = null;
            _exact = null;
        }
    }

    /* -------------------------------------------------------------------------- */
    /*
     * match method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @param value The string to tested
     * @return true if the string matches
     */
    public boolean match(final String value)
    {
        if (_exact != null)
            return _exact.equals(value);
        if (_pattern != null)
            return _pattern.matcher(value).matches();
        return false;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * hashCode method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see java.lang.Object#hashCode()
     * @return the hashcode of this match object
     */
    @Override
    public int hashCode()
    {
        if (_spec == null)
            return 0;
        return _spec.hashCode();
    }

    /* -------------------------------------------------------------------------- */
    /*
     * equals method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @param o The object to be tested
     * @return true if the object o is the same as us
     */
    @Override
    public boolean equals(final Object o)
    {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (o instanceof TwiddleMatch)
        {
            TwiddleMatch other = (TwiddleMatch) o;
            if (_spec == null)
                return (other._spec == null);
            return _spec.equals(other._spec);
        }
        return false;
    }

    /* -------------------------------------------------------------------------- */
    /*
     * toString method
     * /* --------------------------------------------------------------------------
     */
    /**
     * @see java.lang.Object#toString()
     * @return A string for debugging purposes
     */
    @Override
    public String toString()
    {
        if (_exact != null)
            return "exact(" + _exact + ")";
        if (_pattern != null)
            return "regex(" + _pattern.toString() + ")";
        return "none()";
    }
}
