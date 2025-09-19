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

import static jakarta.data.constraint.LikeRecord.CHAR_WILDCARD;
import static jakarta.data.constraint.LikeRecord.ESCAPE;
import static jakarta.data.constraint.LikeRecord.STRING_WILDCARD;
import static jakarta.data.constraint.LikeRecord.translate;

import jakarta.data.expression.TextExpression;
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.StringLiteral;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface NotLike extends Constraint<String> {

    char escape();

    static NotLike literal(String value) {
        Messages.requireNonNull(value, "value");

        StringLiteral expression = StringLiteral.of(LikeRecord.escape(value));

        return new NotLikeRecord(expression, ESCAPE);
    }

    TextExpression<?> pattern();

    static NotLike pattern(String pattern) {
        return pattern(pattern, CHAR_WILDCARD, STRING_WILDCARD);
    }

    static NotLike pattern(String pattern,
                           char charWildcard,
                           char stringWildcard) {
        Messages.requireNonNull(pattern, "pattern");

        StringLiteral expression = StringLiteral.of(translate(pattern,
                                                              charWildcard,
                                                              stringWildcard,
                                                              ESCAPE,
                                                              false));

        return new NotLikeRecord(expression, ESCAPE);
    }

    static NotLike pattern(String pattern,
                           char charWildcard,
                           char stringWildcard,
                           char escape) {
        Messages.requireNonNull(pattern, "pattern");

        StringLiteral expression = StringLiteral.of(translate(pattern,
                                                              charWildcard,
                                                              stringWildcard,
                                                              escape,
                                                              true));
        return new NotLikeRecord(expression, escape);
    }

    static NotLike pattern(TextExpression<?> pattern, char escape) {
        Messages.requireNonNull(pattern, "pattern");

        return new NotLikeRecord(pattern, escape);
    }

    static NotLike prefix(String prefix) {
        Messages.requireNonNull(prefix, "prefix");

        StringLiteral expression = StringLiteral.of(LikeRecord.escape(prefix) +
                                                    STRING_WILDCARD);

        return new NotLikeRecord(expression, ESCAPE);
    }

    static NotLike substring(String substring) {
        Messages.requireNonNull(substring, "substring");

        StringLiteral expression = StringLiteral.of(STRING_WILDCARD +
                                                    LikeRecord.escape(substring) +
                                                    STRING_WILDCARD);

        return new NotLikeRecord(expression, ESCAPE);
    }

    static NotLike suffix(String suffix) {
        Messages.requireNonNull(suffix, "suffix");

        StringLiteral expression = StringLiteral.of(STRING_WILDCARD +
                                                    LikeRecord.escape(suffix));

        return new NotLikeRecord(expression, ESCAPE);
    }

}
