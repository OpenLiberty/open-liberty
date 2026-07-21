/*******************************************************************************
 * Copyright (c) 2010, 2026 IBM Corporation and others.
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
package com.ibm.ws.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

public class WSUtil {
    // Using the same trace style as used by
    // com.ibm.ws.util.ThreadPool
    private static TraceComponent tc =
        Tr.register(WSUtil.class, "Runtime", "com.ibm.ws.runtime.runtime");
    
    // rewrote method to improve perf (251221)    
    @SuppressWarnings("unchecked")
    public static String resolveURI(String uriToResolve) {

        //Not much to resolve in this case
        if (uriToResolve == null) {
            return null;
        }

        int qindex = Integer.MAX_VALUE;
        String qstring = null;
        boolean parsedQuery = false;

        // begin 154268: part 1
        // just to be safe.. convert "\" to "/"
        if (uriToResolve.indexOf('\\') != -1) {
            parsedQuery = true;
            int tmpindex = -1;
            if ((tmpindex = uriToResolve.indexOf('?')) != -1) { // need to remove query string first to avoid altering query params.
                qindex = tmpindex;
                qstring = uriToResolve.substring(qindex);
                uriToResolve = uriToResolve.substring(0, qindex);
                if (uriToResolve == null || uriToResolve.equals("")) { // no uri to resolve; return queryString untouched.
                    return qstring;
                }
            }
            uriToResolve = uriToResolve.replace('\\', '/');
        }
        // end 154268

        // URI contains no metacharacters
        int tmpIndex = -1;
        int testIndex = -1;
        int resolveIndex = Integer.MAX_VALUE; // hold onto index of lowest match --
        // later used to compare against
        // queryString index.
        // 258806: Reduction of one indexOf call in standard uri case
        if ((testIndex = uriToResolve.indexOf("/.")) != -1) {
            if ((tmpIndex = uriToResolve.indexOf("/../", testIndex)) != -1) {
                resolveIndex = tmpIndex;
            }
            if ((tmpIndex = uriToResolve.indexOf("/./", testIndex)) != -1) {
                resolveIndex = resolveIndex < tmpIndex ? resolveIndex : tmpIndex;
            }
        }
        if ((tmpIndex = uriToResolve.indexOf("//")) != -1) {
            resolveIndex = resolveIndex < tmpIndex ? resolveIndex : tmpIndex;
        }

        if (!parsedQuery) {
            if (resolveIndex == Integer.MAX_VALUE) { // no special characters.
                return uriToResolve;
            }
            if ((qindex = uriToResolve.indexOf('?')) != -1) { // need to check for query string.
                qstring = uriToResolve.substring(qindex);
                uriToResolve = uriToResolve.substring(0, qindex);
                if (uriToResolve == null || uriToResolve.equals("")) { // no uri to resolve; return queryString untouched.
                    return qstring;
                }
                if (qindex <= resolveIndex) { // true if query string appears prior to special metacharacters
                    // needing to be resolved.
                    return uriToResolve + qstring;
                }
            }
        } else {
            if (qindex <= resolveIndex) { // true if query string appears prior to special metacharacters needing
                // to be resolved.
                if (qstring == null) {
                    return uriToResolve;
                } else {
                    return uriToResolve + qstring;
                }
            }
        }

        StringTokenizer uriParser = new StringTokenizer(uriToResolve, "/", false);
        String currentElement = null;
        @SuppressWarnings("rawtypes")
        ArrayList uriElements = new ArrayList();

        while (uriParser.hasMoreTokens() == true) {
            currentElement = uriParser.nextToken();

            if ((currentElement == null) || (currentElement.length() < 1)) {
                continue;
            }

            // Attempt to remove the last saved URI element from the list
            if (currentElement.equals("..") == true) {
                if (uriElements.size() < 1) {
                    // URI is outside the current context

                    // Start: Issue 17123:
                    // "UPDATE ERROR OUTPUT TO MEET THE MESSAGING STANDARD" 
                    //
                    // Per the issue, the current text is to be changed to a
                    // generic message, and error details are to be written
                    // to server logs.
                    
                    // throw new java.lang.IllegalArgumentException(
                    //     "The specified URI, " + uriToResolve + ", is invalid because" +
                    //     " it contains more references to parent directories (\"..\")" +
                    //     " than is possible.");

                    if ( TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled() ) {
                        Tr.error(tc,
                            "Non-valid URI [ " + uriToResolve + " ]:" +
                            " The URI contains too many parent directory elements (\"..\")."); 
                    }
                    throw new IllegalArgumentException("Non-valid URI.");

                    // End: Issue 17123
                }

                uriElements.remove(uriElements.size() - 1);

                continue;
            }

            // Ignore, don't add to list of elements
            if (currentElement.equals(".") == true) {
                continue;
            }

            // Keep track of this URI element
            uriElements.add(currentElement);
        }

        // Build a String from the remaining elements of the passed URI

        StringBuffer resolvedURI = new StringBuffer();
        int elementCount = uriElements.size();

        for (int elementIndex = 0; elementIndex < elementCount; ++elementIndex) {
            resolvedURI.append("/" + (String) uriElements.get(elementIndex));
        }

        if (qstring == null) {
            return resolvedURI.toString();
        } else {
            return resolvedURI.toString() + qstring;
        }

    }

    /**
     * Resolves a URI and returns the result truncated at the first semicolon,
     * while validating that the full original URI does not attempt to escape
     * the root context.
     *
     * The method ensures that:
     * - The full original URI is validated (throws exception if it escapes root)
     * - The returned value matches what the web container will use (truncated at ';')
     *
     * @param uri The URI to resolve and truncate
     * @return The resolved URI truncated at the first semicolon (if any)
     * @throws IllegalArgumentException if the URI attempts to escape the root context
     *
     */
    public static String resolveAndTruncateURI(String uri) throws IllegalArgumentException {
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "resolveAndTruncateURI ", uri); 
        }

        // Handle null case
        if (uri == null) {
            return null;
        }

        // Find semicolon and query string positions
        int semiIndex = uri.indexOf(';');
        int qIndex = uri.indexOf('?');

        // no semicolon in URI Path Elements - just call original resolveURI()
        if (semiIndex == -1 || (qIndex != -1 && semiIndex > qIndex)) {
            return resolveURI(uri);
        }

        // If the semicolon is in the last path element we can simply truncate and call the original resolveURI
        // Check if there's a '/' or '\' after the semicolon (but before any query string)
        int slashAfterSemi = uri.indexOf('/', semiIndex);
        int backslashAfterSemi = uri.indexOf('\\', semiIndex);

        // Check if slash/backslash appears before query string (if query string exists)
        boolean hasPathAfterSemi = (slashAfterSemi != -1 && (qIndex == -1 || slashAfterSemi < qIndex)) ||
                                   (backslashAfterSemi != -1 && (qIndex == -1 || backslashAfterSemi < qIndex));

        if (!hasPathAfterSemi) {
            // Semicolon is in last element - safe to just truncate and resolve once
            // No path elements after semicolon means nothing that could escape root
            String truncated = uri.substring(0, semiIndex);
            return resolveURI(truncated);
        }

        // Need to validate the full URI but return a truncated and resolved result
        // path element parameters in the middle of the URI are not common, double resolveURI should be tolerable

        // Validate the full URI (may throw IllegalArgumentException)
        // This preserves the existing exception-throwing behavior for URIs that escape root
        resolveURI(uri);

        // This returns what the web container will actually use
        String truncated = uri.substring(0, semiIndex);
        return resolveURI(truncated);
    }
}
