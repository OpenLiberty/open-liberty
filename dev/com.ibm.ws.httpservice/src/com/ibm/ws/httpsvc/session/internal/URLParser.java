/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.httpsvc.session.internal;

/**
 * Utility class to handle scanning URLs for certain markers. This is a
 * class intended for the session internal code only.
 */
class URLParser {

    protected int idMarker = -1;
    protected int fragmentMarker = -1;
    protected int queryMarker = -1;
    protected int paramMarker = -1;

    /**
     * Parser that handles an HTTP URL and scans for certain markers. These
     * include tracking session id parameters on the last path segment, path
     * fragment markers on that last path, and possible query string data
     * at the end of the URL.
     * 
     * @param url
     * @param target
     *            - search string indicating session information
     */
    URLParser(StringBuffer url, String target) {
        findMarkers(url.toString(), target);
    }

    /**
     * Parser that handles an HTTP URL and scans for certain markers. These
     * include tracking session id parameters on the last path segment, path
     * fragment markers on that last path, and possible query string data
     * at the end of the URL.
     * 
     * @param url
     * @param target
     *            - search string indicating session information
     */
    URLParser(String url, String target) {
        findMarkers(url, target);
    }

    /**
     * Scan the data for various markers, including the provided session id
     * target.
     * 
     * @param url
     * @param target
     */
    private void findMarkers(String url, String target) {
        final char[] data = url.toCharArray();

        // we only care about the last path segment so find that first
        int i = 0;
        int lastSlash = 0;
        for (; i < data.length; i++) {
            if ('/' == data[i]) {
                lastSlash = i;
            } else if ('?' == data[i]) {
                this.queryMarker = i;
                break; // out of loop since query data is after the path
            }
        }
        // now check for the id marker and/or fragments for the last segment
        for (i = lastSlash; i < data.length; i++) {
            if (i == this.queryMarker) {
                // no fragments or segment parameters were found
                break;
            } else if ('#' == data[i]) {
                // found a "#fragment" at the end of the path segment
                this.fragmentMarker = i;
                break;
            } else if (';' == data[i]) {
                // found a segment parameter block (would appear before the
                // optional fragment or query data)
                this.paramMarker = i;
                if (url.regionMatches(i, target, 0, target.length())) {
                    this.idMarker = i;
                }
            }
        }
    }

}
