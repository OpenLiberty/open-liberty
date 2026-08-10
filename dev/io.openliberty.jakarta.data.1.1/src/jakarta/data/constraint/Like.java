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

import jakarta.annotation.Nonnull;
import jakarta.data.expression.TextExpression;
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.StringLiteral;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Like extends Constraint<String> {

    char escape();

    @Nonnull
    static Like literal(@Nonnull String value) {

        Messages.requireNonNull(value, "value");

        StringLiteral expression = StringLiteral.of(LikeRecord.escape(value));

        return new LikeRecord(expression, ESCAPE);
    }

    @Nonnull
    TextExpression<?> pattern();

    @Nonnull
    static Like pattern(@Nonnull String pattern) {

        return pattern(pattern, CHAR_WILDCARD, STRING_WILDCARD);
    }

    @Nonnull
    static Like pattern(@Nonnull String pattern, char charWildcard, char stringWildcard) {
        Messages.requireNonNull(pattern, "pattern");

        StringLiteral expression = StringLiteral.of(translate(pattern,
                                                              charWildcard,
                                                              stringWildcard,
                                                              ESCAPE,
                                                              false));

        return new LikeRecord(expression, ESCAPE);
    }

    @Nonnull
    static Like pattern(@Nonnull String pattern,
                        char charWildcard,
                        char stringWildcard,
                        char escape) {

        Messages.requireNonNull(pattern, "pattern");

        StringLiteral expression = StringLiteral.of(translate(pattern,
                                                              charWildcard,
                                                              stringWildcard,
                                                              escape,
                                                              true));

        return new LikeRecord(expression, escape);
    }

    @Nonnull
    static Like pattern(@Nonnull TextExpression<?> pattern, char escape) {

        Messages.requireNonNull(pattern, "pattern");

        return new LikeRecord(pattern, escape);
    }

    @Nonnull
    static Like prefix(@Nonnull String prefix) {

        Messages.requireNonNull(prefix, "prefix");

        StringLiteral expression = StringLiteral //
                        .of(LikeRecord.escape(prefix) + STRING_WILDCARD);

        return new LikeRecord(expression, ESCAPE);
    }

    @Nonnull
    static Like substring(@Nonnull String substring) {

        Messages.requireNonNull(substring, "substring");

        StringLiteral expression = StringLiteral.of(STRING_WILDCARD +
                                                    LikeRecord.escape(substring) +
                                                    STRING_WILDCARD);

        return new LikeRecord(expression, ESCAPE);
    }

    @Nonnull
    static Like suffix(@Nonnull String suffix) {

        Messages.requireNonNull(suffix, "suffix");

        StringLiteral expression = StringLiteral.of(STRING_WILDCARD +
                                                    LikeRecord.escape(suffix));

        return new LikeRecord(expression, ESCAPE);
    }

}
