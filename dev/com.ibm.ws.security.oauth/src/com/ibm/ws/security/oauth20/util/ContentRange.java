/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The abstract base class for representing weighted types typically found in the
 * content negotiation "accept-*" and "content-*" headers for RFC2616.
 *
 * @since 1.0
 */
public abstract class ContentRange implements Comparable<ContentRange> {

    /**
     * Returns the type enclosed by this content range.
     *
     * @return The type of this content range. Never <code>null</code>.  The
     * returned string will be normalized to lowercase from its original input value.
     */
    public String getType() {
        return _type;
    }

    /**
     * Returns the 'q' ("quality") value for this type, as described in Section 3.9 of
     * RFC2616.  The domain is "short floating point", with a range of 0.0
     * to 1.0, with 0.0 as "unacceptable" and 1.0 as "most acceptable".
     *
     * @return The 'q' value for this type. Never <code>null</code>.  If 'q' value
     * doesn't make sense for the context (e.g. this range was extracted from a
     * "content-*" header, as opposed to "accept-*" header, its value will always
     * be "1".
     */
    public Float getQValue() {
        return _qValue;
    }

    /**
     * Creates a ContentRange object with the referenced values.
     *
     * @param type The type of this content range. May be <code>null</code>.
     * Setting to <code>null</code> or the empty string is the equivalent to
     * setting to "*" (all types). The string is normalized to lowercase and all
     * LWS removed.
     *
     * @param qValue The quality value of this range.  May be <code>null</code>.
     * Note that the permissible range of values are 0 to 1.0. Setting to
     * <code>null</code> is the equivalent to setting a quality value
     * of '1.0'.
     */
    public ContentRange(String type, Float qValue)
    {
        if (type == null || type.length() == 0)
            type = "*"; //$NON-NLS-1$

        _type = type.toLowerCase().trim();

        if (qValue == null)
            _qValue = new Float(1.0);
        else if (qValue < 0 || qValue > 1)
            throw new NumberFormatException("qValue cannot be less than '0' or greater than '1.0'"); //$NON-NLS-1$
        else
            _qValue = qValue;
    }

    /**
     * The interface called by {@link ContentRange#parse(String, com.ibm.team.jfs.app.http.util.ContentRange.RangeParseCallback)}
     * during significant points in the parse.
     */
    protected interface RangeParseCallback {

        /**
         * Creates a ContentRange derivation. Called when a ContentRange has
         * been parsed.
         *
         * @param type The type to create. May be <code>null</code> or empty.
         * If non-<code>null</code> the name is normalized to lowercase with
         * leading/trailing whitespace trimmed.
         *
         * @param parameters The optional parameters to the type. May be <code>null</code>.
         * If non-<code>null</code> the name/values are normalized to lowercase with
         * leading/trailing whitespace trimmed.
         *
         * @param qValue The quality value for the type. May be <code>null</code>.
         *
         * @param extensions The extensions to the quality value. May be <code>null</code>.
         * If non-<code>null</code> the name/values are normalized to lowercase with
         * leading/trailing whitespace trimmed.
         *
         * @return The created ContentRange.  May be <code>null</code> to indicate
         * that a content range should not be created and added to the parsed ranges.
         */
        ContentRange rangeParsed(String type, HashMap<String, String[]> parameters,
                Float qValue, HashMap<String, String[]> extensions);

        /**
         * Called prior to sorting a completed range.  Implementations have the opportunity
         * to modify the referenced collection prior to the sort.
         *
         * @param range The completed range, prior to sorting. Never <code>null</code>.
         */
        void preSort(ArrayList<ContentRange> range);

        /**
         * Called immediately after sorting a completed range, but prior to
         * returning.  Implementations have the opportunity to modify the referenced
         * sorted collection prior to it being returned from the parse.
         *
         * @param range The completed range, immediately after sorting but prior
         * to returning. Never <code>null</code>.
         */
        void postSort(ArrayList<ContentRange> range);
    }

    /**
     * Parses an Accept* header value string, and returns the set of ContentRanges
     * that describes the values parsed.
     *
     * @param value The header value string to be parsed.  May be <code>null</code> or
     * empty.
     *
     * @param cb The interface callback to call for creating and sorting
     * ContentRange derivations. Must not be <code>null</code>.
     *
     * @return The sorted ContentRanges.  Never <code>null</code>.
     */
    static protected ContentRange[] parse(String value, RangeParseCallback cb) {

        if (cb == null)
            throw new IllegalArgumentException("RangeParseCallback must not be null"); //$NON-NLS-1$

        ArrayList<ContentRange> ranges = new ArrayList<ContentRange>(5);

        if (value == null)
            value = ""; //$NON-NLS-1$
        if (value.length() == 0) {
            ContentRange range = cb.rangeParsed(null, null, null, null);
            if (range != null)
                ranges.add(range);
        }

        StringTokenizer rangeTokenizer = new StringTokenizer(value, ","); //$NON-NLS-1$
        while (rangeTokenizer.hasMoreTokens()) {
            StringTokenizer parmTokenizer = new StringTokenizer(rangeTokenizer.nextToken(), ";"); //$NON-NLS-1$
            if (parmTokenizer.countTokens() == 0)
                continue;

            // There is at least a type.
            String type = parmTokenizer.nextToken().toLowerCase().trim();
            Float qValue = null;
            HashMap<String, String[]> parameters = null;
            HashMap<String, String[]> extensions = null;

            int numTokens = parmTokenizer.countTokens();
            if (numTokens == 0) {
                // Only the type of the range is specified
                ContentRange range = cb.rangeParsed(type, parameters, qValue, extensions);
                if (range != null)
                    ranges.add(range);
                continue;
            }

            // There are either parameters or a qvalue next
            String nextToken = parmTokenizer.nextToken().toLowerCase();
            String[] parm = StringUtil.splitPair(nextToken, '=');
            if (parm[0].equalsIgnoreCase("q")) { //$NON-NLS-1$
                qValue = new Float(parm[1]);
            }
            else {
                parameters = new HashMap<String, String[]>();
                parameters.put(parm[0].toLowerCase(), new String[] { parm[1].toLowerCase() });
                while (parmTokenizer.hasMoreTokens()) {
                    nextToken = parmTokenizer.nextToken().toLowerCase();
                    parm = StringUtil.splitPair(nextToken, '=');
                    if (parm[0].equalsIgnoreCase("q")) { //$NON-NLS-1$
                        qValue = new Float(parm[1]);
                        break;
                    }
                    ContentRange.updateParmMap(parameters, parm);
                }
            }

            if (parmTokenizer.hasMoreTokens()) {
                // If we got here, then we're parsing the q-value extensions
                extensions = new HashMap<String, String[]>();
                while (parmTokenizer.hasMoreTokens()) {
                    nextToken = parmTokenizer.nextToken().toLowerCase();
                    parm = StringUtil.splitPair(nextToken, '=');
                    ContentRange.updateParmMap(extensions, parm);
                }
            }

            // Finally, add the new ContentRange to the list
            ContentRange range = cb.rangeParsed(type, parameters, qValue, extensions);
            if (range != null)
                ranges.add(range);
        }

        // Sort the list prior to return.
        cb.preSort(ranges);
        Collections.sort(ranges);
        cb.postSort(ranges);
        return ranges.toArray(new ContentRange[ranges.size()]);
    }

    /**
     * Updates a parameter map (parameter as described in RFC2616 as token=token pair)
     * with a parameter token.  If the LHS of the parameter is already in the map, then
     * the maps set of values is appended to with the parameter RHS, else a new
     * name/value pair is added to the map.  The name/values are normalized to lower-case.
     *
     * @param parmMap The parameter map to update. Must not be <code>null</code>.
     *
     * @param token The token=token token, tokenized into a name/value pair (name
     * at array position '0', value at '1'). Must not be <code>null</code>.
     */
    static private void updateParmMap(Map<String, String[]> parmMap, String[] parm) {

        String name = parm[0].toLowerCase();
        String value = parm[1].toLowerCase();

        if (parmMap.containsKey(name)) {
            String[] values = parmMap.get(name);
            String[] newVals = (String[]) Array.newInstance(String.class, values.length + 1);
            System.arraycopy(values, 0, newVals, 0, values.length);
            newVals[newVals.length - 1] = value;
            parmMap.put(name, newVals);
        }
        else {
            parmMap.put(name, new String[] { value });
        }
    }

    /**
     * ContentRanges are considered equal if their qValues and types
     * compare equal (case-insensitive compare for type).
     *
     * @return <code>true</code>if the qValues and types compare equal,
     * <code>false</code> if not.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ContentRange))
            return false;
        if (this == o)
            return true;

        ContentRange that = (ContentRange) o;
        return _qValue.equals(that._qValue) && _type.equalsIgnoreCase(that._type);
    }

    /**
     * Returns a hash based on this instance's <code>_type</code>.
     *
     * @return Returns the hashcode of the instance's type.
     */
    @Override
    public int hashCode() {
        return _type.hashCode();
    }

    /**
     * Provides a string representation of this media range, suitable for
     * use as an Accept header value.  Note that the literal text values used
     * to create this range may be normalized to lowercase on construction, and
     * will be represented as such here - the values may not compare equal
     * wrt case-sensitivity with the values used to create this instance.
     *
     * @return A media range suitable for use as an Accept header value.
     * Never <code>null</code>.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append(_type);

        /*
         * '1' is equivalent to specifying no qValue.
         */
        if (_qValue.floatValue() != 1.0)
            buf.append(String.format(";q=%s", _qValue.toString())); //$NON-NLS-1$

        return buf.toString();
    }

    /**
     * Compares two ContentRanges for equality.  The values are first compared
     * according to <code>qValue</code> values.  Should those values be equal,
     * the <code>type</code> is then lexicographically compared (case-insensitive)
     * in ascending order, with the "*" type demoted last in that order.
     *
     * @param that The range to compare to.  If <code>null</code> or not of
     * type ContentRange, this instance is promoted.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ContentRange that) {

        if (that == null)
            return 1;

        float lhs = _qValue.floatValue();
        float rhs = that._qValue.floatValue();

        if (lhs == rhs) {
            String thisType = _type;
            String thatType = that._type;
            if (thisType.equals(thatType))
                return 0;
            else if (thisType.equals("*")) //$NON-NLS-1$
                return 1;
            else if (thatType.equals("*")) //$NON-NLS-1$
                return -1;
            else
                return thisType.compareTo(thatType);
        }
        else if (lhs < rhs)
            return 1;
        else
            return -1;
    }

    /**
     * The type enclosed by this content range.
     */
    final protected String _type;

    /**
     * The 'q' ("quality") value for this type.
     */
    final protected Float _qValue;

}
