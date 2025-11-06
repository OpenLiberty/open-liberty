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
package jakarta.data.spi.expression.function;

import java.util.List;

import jakarta.data.expression.ComparableExpression;
import jakarta.data.expression.TextExpression;
import jakarta.data.messages.Messages;
import jakarta.data.spi.expression.literal.NumericLiteral;
import jakarta.data.spi.expression.literal.StringLiteral;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface TextFunctionExpression<T> extends //
                FunctionExpression<T, String>, //
                TextExpression<T> {

    String CONCAT = "concat";
    String LEFT = "left";
    String LOWER = "lower";
    String RIGHT = "right";
    String UPPER = "upper";

    @Override
    List<? extends ComparableExpression<? super T, ?>> arguments();

    static <T> TextFunctionExpression<T> of(String name,
                                            String left,
                                            TextExpression<? super T> right) {
        Messages.requireNonNull(left, "left");

        Messages.requireNonNull(right, "right");

        return new TextFunctionExpressionRecord<>(name, //
                        List.of(StringLiteral.of(left), right));
    }

    static <T> TextFunctionExpression<T> of(String name,
                                            TextExpression<? super T> expression) {
        Messages.requireNonNull(expression, "expression");

        return new TextFunctionExpressionRecord<>(//
                        name, //
                        List.of(expression));
    }

    static <T> TextFunctionExpression<T> of(String name,
                                            TextExpression<? super T> left,
                                            int literal) {
        Messages.requireNonNull(left, "left");

        return new TextFunctionExpressionRecord<>(//
                        name, //
                        List.of(left, NumericLiteral.of(literal)));
    }

    static <T> TextFunctionExpression<T> of(String name,
                                            TextExpression<? super T> left,
                                            String right) {
        Messages.requireNonNull(left, "left");

        Messages.requireNonNull(right, "right");

        return new TextFunctionExpressionRecord<>(//
                        name, //
                        List.of(left, StringLiteral.of(right)));
    }

    static <T> TextFunctionExpression<T> of(String name,
                                            TextExpression<? super T> left,
                                            TextExpression<? super T> right) {
        Messages.requireNonNull(left, "left");

        Messages.requireNonNull(right, "right");

        return new TextFunctionExpressionRecord<>(//
                        name, //
                        List.of(left, right));
    }

}
