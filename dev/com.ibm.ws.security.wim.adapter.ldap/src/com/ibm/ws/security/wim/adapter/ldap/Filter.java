/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap;

import java.util.ArrayList;

class Filter {

    final static String delimiters = " \t\r\n";

    static class SubExpr {
        static final int ALL = -1;
        public int start = 0; // Starting token [0-based]
        public int end = ALL; // -1 = ALL, otherwise ending token [0-based]
    }

    private final ArrayList<Object> expression = new ArrayList<Object>(3);
    private String filterString = null;

    public Filter(String filter) {
        /*
         * Create the filter to use in the search.
         * The 'template' can be of the forms:
         * %v - all words
         * %v1 - 1st word
         * %v1-2 - words 1 through 2
         * %v2- - words 2 through last word
         */
        if (filter == null)
            return;

        filterString = filter;

        int idx = 0;
        while (idx != -1 && idx < filter.length()) {
            int begin = idx;
            idx = filter.indexOf("%v", idx);
            if (idx != -1) {
                expression.add(filter.substring(begin, idx));
                SubExpr tok = new SubExpr();
                expression.add(tok);

                idx += 2;

                int digit = 0;
                while (idx < filter.length() &&
                       Character.isDigit(filter.charAt(idx))) {
                    digit = 10 * digit + Character.digit(filter.charAt(idx), 10);
                    idx++;
                }
                if (digit != 0) {
                    // found a number
                    digit--;
                    tok.end = digit;
                }
                tok.start = digit;

                // check for - separator
                if (idx < filter.length() &&
                    filter.charAt(idx) == '-') {
                    tok.end = SubExpr.ALL;
                    idx++;
                    digit = 0;
                    while (idx < filter.length() &&
                           Character.isDigit(filter.charAt(idx))) {
                        digit = 10 * digit + Character.digit(filter.charAt(idx), 10);
                        idx++;
                    }

                    if (digit != 0) {
                        // found a number
                        tok.end = digit - 1;
                    }
                }
            } else {
                expression.add(filter.substring(begin));
            }
        }
    }

    public String prepare(String toks) {

        ArrayList<SubExpr> tokens = new ArrayList<SubExpr>(3);
        int idx = 0;
        while (idx != -1) {
            // skip the delimiters
            idx = skipDelimiters(toks, idx);
            // if all delimiters then stop
            if (idx == -1)
                break;

            SubExpr se = new SubExpr();
            // mark the beginning of the token
            se.start = idx;
            tokens.add(se);
            idx = skipNonDelimiters(toks, idx);
            // if we go past the end, just mark it as the end
            if (idx == -1)
                idx = toks.length();
            // mark the end of the token (exclusive)
            se.end = idx;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expression.size(); i++) {
            Object o = expression.get(i);
            if (o instanceof SubExpr) {
                SubExpr t = (SubExpr) o;

                // if the start token is invalid, skip the subexpression
                if (t.start >= tokens.size())
                    continue;

                int endIdx = t.end;
                // make sure that the end index is no bigger than the input length
                if ((endIdx == -1) ||
                    (endIdx >= tokens.size()))
                    endIdx = tokens.size() - 1;

                // find the start and end positions and append the substring
                SubExpr begin = tokens.get(t.start);
                SubExpr end = tokens.get(endIdx);
                sb.append(toks, begin.start, end.end);
            } else {
                sb.append(o);
            }
        }
        return sb.toString();
    }

    private int skipDelimiters(String token, int idx) {
        if (token == null)
            return -1;

        int len = token.length();

        while ((idx < token.length()) &&
               (delimiters.indexOf(token.charAt(idx)) >= 0)) {
            idx++;
        }
        if (idx == len)
            idx = -1;
        return idx;
    }

    private int skipNonDelimiters(String token, int idx) {
        int len = token.length();

        while ((idx < token.length()) &&
               (delimiters.indexOf(token.charAt(idx)) < 0)) {
            idx++;
        }
        if (idx == len)
            idx = -1;
        return idx;
    }

    @Override
    public String toString() {
        return filterString;
    }

}
