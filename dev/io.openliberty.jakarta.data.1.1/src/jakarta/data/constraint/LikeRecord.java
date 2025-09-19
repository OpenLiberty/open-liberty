/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package jakarta.data.constraint;

import jakarta.data.expression.TextExpression;

/**
 * Method signatures are copied from Jakarta Data.
 */
record LikeRecord(TextExpression<?> pattern, char escape)
                implements Like {

    static final char CHAR_WILDCARD = '_';
    static final char ESCAPE = '\\';
    static final char STRING_WILDCARD = '%';

    static String escape(String literal) {
        StringBuilder s = new StringBuilder();

        for (int c = 0; c < literal.length(); c++) {
            char ch = literal.charAt(c);
            if (ch == CHAR_WILDCARD ||
                ch == ESCAPE ||
                ch == STRING_WILDCARD)
                s.append(ESCAPE);

            s.append(ch);
        }

        return s.toString();
    }

    @Override
    public NotLike negate() {
        return new NotLikeRecord(pattern, escape);
    }

    @Override
    public String toString() {
        return "LIKE " + pattern + " ESCAPE '" + escape + "'";
    }

    static String translate(String pattern,
                            char charWildcard,
                            char stringWildcard,
                            char escape,
                            boolean patternIsAlreadyEscaped) {

        if (charWildcard == stringWildcard) {
            throw new IllegalArgumentException(Character.toString(charWildcard));
        }

        if (charWildcard == escape || stringWildcard == escape) {
            throw new IllegalArgumentException(Character.toString(escape));
        }

        StringBuilder result = new StringBuilder();

        boolean previousCharIsEscape = false;
        for (int i = 0; i < pattern.length(); i++) {
            final char ch = pattern.charAt(i);
            if (previousCharIsEscape) {
                if (ch == CHAR_WILDCARD ||
                    ch == STRING_WILDCARD ||
                    ch == escape) {
                    result.append(escape);
                }
                result.append(ch);
                previousCharIsEscape = false;
            } else if (ch == charWildcard) {
                result.append(CHAR_WILDCARD);
            } else if (ch == stringWildcard) {
                result.append(STRING_WILDCARD);
            } else if (ch == escape && patternIsAlreadyEscaped) {
                previousCharIsEscape = true;
            } else {
                if (ch == STRING_WILDCARD ||
                    ch == CHAR_WILDCARD ||
                    ch == escape) {
                    result.append(escape);
                }
                result.append(ch);
            }
        }

        if (previousCharIsEscape)
            throw new IllegalArgumentException("pattern: " + pattern);

        return result.toString();
    }
}
