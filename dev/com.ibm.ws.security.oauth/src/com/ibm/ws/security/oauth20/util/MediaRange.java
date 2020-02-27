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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Describes a single type used in content negotiation
 * between an HTTP client and server, as described in Section 14.1 and 14.7 of RFC2616
 * (the HTTP/1.1 specification).
 * 
 * @since 1.0
 */
public class MediaRange extends ContentRange {

    /**
     * Returns the type enclosed by this media range. Examples of such a type might be
     * text/html, text/*, *\/*.
     * 
     * @return The type of this media range. Never <code>null</code>. The
     *         the returned string will be normalized to lowercase from its original input
     *         value.
     */
    @Override
    public String getType() {
        return super.getType();
    }

    /**
     * Returns the optional set of parameters associated to the type as returned
     * by {@link #getType()}. The parameters are those values as described
     * in standardized MIME syntax. An example of such a parameter in string form
     * might be "level=1".
     * 
     * @return The optional list of parameters. Neither the return value or
     *         the map values will ever be <code>null</code>. Depending upon how this type
     *         was constructed, the returned string values may be normalized to lowercase from
     *         their original input value.
     */
    public Map<String, String[]> getParameters() {
        return _parameters;
    }

    /**
     * Returns the optional set of custom extensions defined for this type.
     * 
     * @return The optional list of extensions. Neither the return value or
     *         the map values will ever be <code>null</code>. Depending upon how this type
     *         was constructed, the returned string values may be normalized to lowercase from
     *         their original input value.
     */
    public Map<String, String[]> getExtensions() {
        return _extensions;
    }

    /**
     * Provides a string representation of this media range, suitable for
     * use as an Accept header value. Note that the literal text values used
     * to create this range may be normalized to lowercase on construction, and
     * will be represented as such here - the values may not compare equal
     * wrt case-sensitivity with the values used to create this instance.
     * 
     * @return A media range suitable for use as an Accept header value.
     *         Never <code>null</code>.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(_type);
        if (!_parameters.isEmpty()) {
            for (Entry<String, String[]> entry : _parameters.entrySet()) {
                buf.append(';');
                buf.append(String.format("%s=%s", entry.getKey(), entry.getValue())); //$NON-NLS-1$
            }
        }

        /*
         * '1' is equivalent to specifying no qValue. If there's no extensions,
         * then we won't include a qValue.
         */
        if (_qValue.floatValue() == 1.0) {
            if (!_extensions.isEmpty()) {
                buf.append(String.format(";q=%s", _qValue.toString())); //$NON-NLS-1$
                for (Entry<String, String[]> entry : _extensions.entrySet()) {
                    buf.append(';');
                    buf.append(String.format("%s=%s", entry.getKey(), entry.getValue())); //$NON-NLS-1$
                }
            }
        }
        else {
            buf.append(String.format(";q=%s", _qValue.toString())); //$NON-NLS-1$
            for (Entry<String, String[]> entry : _extensions.entrySet()) {
                buf.append(';');
                buf.append(String.format("%s=%s", entry.getKey(), entry.getValue())); //$NON-NLS-1$
            }
        }
        return buf.toString();
    }

    /**
     * MediaRanges are considered equal if their qValues, types, and extensions
     * compare equal (case-insensitive compare for type).
     * 
     * @return <code>true</code>if the qValues, types, and extensions compare equal,
     *         <code>false</code> if not.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof MediaRange))
            return false;
        if (this == o)
            return true;

        MediaRange that = (MediaRange) o;
        return _qValue.equals(that._qValue) &&
                _type.equalsIgnoreCase(that._type) &&
                MediaRange.parmMapsEqual(_parameters, that._parameters) &&
                MediaRange.parmMapsEqual(_extensions, that._extensions);
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
     * Compares two maps for equality.
     * The assumption is that the name/values of the parameters have been
     * case-normalized for comparison purposes (see ContentRange#updateParmMap).
     * 
     * @param thisMap May be <code>null</code>.
     * @param thatMap May be <code>null</code>.
     * @return <code>true</code> if the maps contain the same keys and values,
     *         <code>false</code> if not. If both maps are <code>null</code> <code>true</code>
     *         is returned.
     */
    static private boolean parmMapsEqual(Map<String, String[]> thisMap, Map<String, String[]> thatMap) {

        if (thatMap == null && thisMap == null)
            return true;

        if (thatMap == null && thisMap != null || thatMap != null && thisMap == null)
            return false;

        for (Entry<String, String[]> entry : thatMap.entrySet()) {
            String thatKey = entry.getKey();
            String[] thatVal = entry.getValue();

            String[] thisVal = thisMap.get(thatKey);
            if (thisVal == null)
                return false;

            if (!MediaRange.arraysEqual(thisVal, thatVal))
                return false;
        }

        for (Entry<String, String[]> entry : thisMap.entrySet()) {
            String thisKey = entry.getKey();
            String[] thisVal = entry.getValue();

            String[] thatVal = thatMap.get(thisKey);
            if (thatVal == null)
                return false;

            if (!MediaRange.arraysEqual(thatVal, thisVal))
                return false;
        }

        return true;
    }

    /**
     * Compares the referenced string arrays for equality.
     * Assumes that the values for the arrays have been case-normalized.
     * 
     * @param a May be <code>null</code>.
     * @param b May be <code>null</code>.
     * @return <code>true</code> if the referenced arrays are equal in length
     *         and contain the same elements, <code>false</code> if not. If both
     *         arrays are <code>null</code> <code>true</code> is returned.
     */
    static private boolean arraysEqual(String[] a, String[] b) {

        if (a == null && b == null)
            return true;
        if (a != null && b == null || a == null && b != null)
            return false;

        Arrays.sort(a);
        Arrays.sort(b);

        return Arrays.equals(a, b);
    }

    /**
     * Creates a MediaRange object with the referenced values.
     * 
     * @param type The MIME tpe of this media range. May be <code>null</code>.
     *            Setting to <code>null</code> or the empty string is the equivalent to
     *            setting to "*\/*" (all types). The string is normalized to lowercase and
     *            all LWS removed.
     * 
     * @param parameters The optional parameters for this range. May be
     *            <code>null</code>.
     * 
     * @param qValue The quality value of this range. May be <code>null</code>.
     *            Note that the permissible range of values are 0 to 1.0. Setting to
     *            <code>null</code> is the equivalent to setting a quality value
     *            of '1.0'.
     * 
     * @param extensions The optional extensions to this quality value. May
     *            be <code>null</code>.
     */
    public MediaRange(String type, HashMap<String, String[]> parameters,
            Float qValue, HashMap<String, String[]> extensions)
    {
        super(type == null || type.length() == 0 ? "*/*" : type, qValue); //$NON-NLS-1$

        if (parameters == null)
            _parameters = new HashMap<String, String[]>();
        else
            _parameters = parameters;

        if (extensions == null)
            _extensions = new HashMap<String, String[]>();
        else
            _extensions = extensions;
    }

    /**
     * Parses an Accept header value into an array of media ranges. The
     * returned media ranges are sorted such that the most acceptable media
     * is available at ordinal position '0', and the least acceptable at
     * position n-1.<p/>
     * 
     * The syntax expected to be found in the referenced <code>value</code>
     * complies with the syntax described in RFC2616, Section 14.1, as
     * described below: <p/>
     * 
     * <code>
     * Accept = "Accept" ":"
     * #( media-range [ accept-params ] )
     * 
     * media-range = ( "*\/*"
     * | ( type "/" "*" )
     * | ( type "/" subtype )
     * ) *( ";" parameter )
     * accept-params = ";" "q" "=" qvalue *( accept-extension )
     * accept-extension = ";" token [ "=" ( token | quoted-string ) ]
     * </code>
     * 
     * @param value The value to parse. May be <code>null</code> or empty.
     *            If <code>null</code> or empty, a single MediaRange is returned that represents
     *            all types.
     * 
     * @return The media ranges described by the string. The ranges
     *         are sorted such that the most acceptable media is available at ordinal
     *         position '0', and the least acceptable at position n-1.
     */
    static public MediaRange[] parse(String value) {

        RangeParseCallback cb = new RangeParseCallback() {

            public ContentRange rangeParsed(
                    String type, HashMap<String, String[]> parameters,
                    Float qValue, HashMap<String, String[]> extensions)
            {
                return new MediaRange(type, parameters, qValue, extensions);
            }

            public void preSort(ArrayList<ContentRange> rangeList) {/* empty */
            }

            public void postSort(ArrayList<ContentRange> range) {/* empty */
            }
        };

        ContentRange[] range = ContentRange.parse(value, cb);
        MediaRange[] newRange = (MediaRange[]) Array.newInstance(MediaRange.class, range.length);
        System.arraycopy(range, 0, newRange, 0, range.length);

        return newRange;
    }

    /**
     * Compares two MediaRanges for equality. The values are first compared
     * according to <code>qValue</code> values. Should those values be equal,
     * the <code>type</code> is then lexicographically compared (case-insensitive)
     * in ascending order, with the "*" type demoted last in that order.
     * MediaRanges with the same type but different sub-types are compared - a more
     * specific subtype is promoted over the 'wildcard' subtype. MediaRanges with
     * the same types but with extensions are promoted over those same types
     * with no extensions.</p>
     * 
     * Note that parameters nor extensions are considered in the comparison.
     * 
     * @param that The range to compare to. If <code>null</code> or not of
     *            type MediaRange, this instance is promoted.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ContentRange that) {

        if (that == null || !(that instanceof MediaRange))
            return 1;

        float lhs = _qValue.floatValue();
        float rhs = that._qValue.floatValue();

        if (lhs == rhs) {
            String[] thisType = StringUtil.splitAcceptPairAllowingSingleAsterisk(_type);
            String[] thatType = StringUtil.splitAcceptPairAllowingSingleAsterisk(that._type);
            if (thisType[0].equalsIgnoreCase(thatType[0])) {
                if (thisType[1].equalsIgnoreCase(thatType[1]))
                    return MediaRange.compareParms(this, (MediaRange) that);
                else if (thisType[1].equals("*")) //$NON-NLS-1$
                    return 1;
                else if (thatType[1].equals("*")) //$NON-NLS-1$
                    return -1;
                else
                    return thisType[1].compareTo(thatType[1]);
            }
            else if (thisType[0].equals("*")) //$NON-NLS-1$
                return 1;
            else if (thatType[0].equals("*")) //$NON-NLS-1$
                return -1;
            else
                return _type.compareTo(that._type);
        }
        else if (lhs < rhs)
            return 1;
        else
            return -1;
    }

    /**
     * Determine whether a set of parameters matches those in this media range.
     * <p>
     * For each parameter, if the media range specifies the same parameter but with
     * a different value, then there is no match, and this method returns <code>false</code>.
     * If the media range specifies either no value or a matching value for each
     * parameter, then <code>true</code> is returned.
     * 
     * @param params The parameters to check, in the format of "name=value" strings
     * @return <code>true</code> if the media range specifies no value or a matching value
     *         for each parameter; <code>false</code> otherwise.
     */
    private boolean matchesParms(List<String> params) {
        Map<String, String[]> parmMap = getParameters();
        for (String parm : params) {
            String[] splitParm = parm.split("=", 2); //$NON-NLS-1$
            if (splitParm.length != 2)
                return false;
            String parmName = splitParm[0].toLowerCase().trim();
            String parmValue = splitParm[1].toLowerCase().trim();
            if (parmMap.containsKey(parmName)) {
                boolean valueFound = false;
                for (String value : parmMap.get(parmName)) {
                    if (parmValue.equals(value)) {
                        valueFound = true;
                        break;
                    }
                }
                if (!valueFound)
                    return false;
            }
        }
        return true;
    }

    /**
     * Compares the parameters/extensions of the referenced media ranges.
     * 
     * Section 14.1 of RFC2616 says:
     * "Media ranges can be overridden by more specific media ranges or specific media types.
     * If more than one media range applies to a given type, the most specific reference
     * has precedence." It does not state that there needs to be any
     * lexicographical ordering of the qualifiers that boost one type to be more specific
     * than another - apparently just the presence of the qualifiers is sufficient.<p/>
     * 
     * In this function the presence of type parameters or extensions causes one
     * MediaRange to have precedence over another and forces the compare accordingly.
     * The algorithm first checks if any parameters or extensions are defined for either
     * MediaRange. If not, '0' is returned. If one MediaRange has a parameter
     * defined and the other one doesn't, that MediaRange is preferred and the function
     * returns '1' or '-1' appropriately. If one MediaRange has an extension defined
     * but the other one doesn't, that MediaRange is preferred and the function returns
     * '1' or '-1' appropriately. If both MediaRanges have both parameters and extensions
     * defined, then '0' is returned.
     * 
     * @param a May be <code>null</code>.
     * @param b May be <code>null</code>.
     * @return '0' if both compare equal, '-1' if <code>a</code> is more "specific"
     *         (so that it will be arrive first in an ascending sort), '1' if <code>b</code>
     *         is more specific.
     */
    static private int compareParms(MediaRange a, MediaRange b) {
        if ((a._parameters == null || a._parameters.isEmpty()) &&
                (b._parameters == null || b._parameters.isEmpty()) &&
                (a._extensions == null || a._extensions.isEmpty()) &&
                (b._extensions == null || b._extensions.isEmpty()))
        {
            return 0;
        }

        if (a._parameters != null && !a._parameters.isEmpty() &&
                (b._parameters == null || b._parameters.isEmpty()))
        {
            return -1;
        }

        if (b._parameters != null && !b._parameters.isEmpty() &&
                (a._parameters == null || a._parameters.isEmpty()))
        {
            return 1;
        }

        if (a._extensions != null && !a._extensions.isEmpty() &&
                (b._extensions == null || b._extensions.isEmpty()))
        {
            return -1;
        }

        if (b._extensions != null && !b._extensions.isEmpty() &&
                (a._extensions == null || a._extensions.isEmpty()))
        {
            return 1;
        }

        return 0;
    }

    private static boolean allMediaRangesHaveQValue1(MediaRange[] mediaRanges) {
        for (MediaRange mediaRange : mediaRanges) {
            if (mediaRange.getQValue() != 1f) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Returns the {@link MediaRange#getQValue() q value} for the specified media range,
     * according to Apache's content negotiation algorithm: if all media type of a request
     * have the default value (1), '* /*' is 0.1 and 'foo/*' is 0.2. If one media type
     * has a non-default value, then all values are used as specified on the request.</p>
     * 
     * @param mediaRange the media range
     * @param allMediaRangeHaveQValue1 boolean that indicates if all media ranges of a
     *            request have the default q value (1)
     * @return the computed q value
     * 
     * @since 3.0
     * @author Analysis Team
     */
    public static float getQValue(MediaRange mediaRange, boolean allMediaRangeHaveQValue1) {
        return !allMediaRangeHaveQValue1 || mediaRange.getQValue() != 1f ? mediaRange.getQValue() : "*/*".equals(mediaRange.getType()) ? 0.1f : //$NON-NLS-1$
                mediaRange.getType().endsWith("/*") ? 0.2f : //$NON-NLS-1$
                        1f;
    }

    /**
     * <p>Returns whether the specified {@link ContentRange content range}s match, ie, if their {@link ContentRange#getType() type}s are the same or if one encompasses the other
     * (like 'application/xml' and 'application/*').</p>
     * 
     * @param contentRange1 a content range
     * @param contentRange2 another content range
     * @return whether the content ranges matches
     * 
     * @since 3.0
     * @author Analysis Team
     */
    public static boolean contentRangesMatch(ContentRange contentRange1, ContentRange contentRange2) {
        if (contentRange1 == contentRange2 ||
                (contentRange1.getType() == null ? contentRange2.getType() == null : contentRange1.getType().equals(contentRange2.getType()))
                || "*/*".equals(contentRange1.getType()) || "*/*".equals(contentRange2.getType())) { //$NON-NLS-1$ //$NON-NLS-2$
            return true;
        }

        if (contentRange1.getType().charAt(contentRange1.getType().length() - 1) == '*' || contentRange2.getType().charAt(contentRange2.getType().length() - 1) == '*') {
            String mainType1 = contentRange1.getType().substring(0, contentRange1.getType().indexOf('/'));
            return contentRange2.getType().startsWith(mainType1 + '/');
        }

        return false;
    }

    final private HashMap<String, String[]> _parameters;
    final private HashMap<String, String[]> _extensions;
}
