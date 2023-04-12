/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.data.repository;

/**
 * Update operations for {@link Update#op()}.
 */
public enum Operation {
    /**
     * <p>The {@code Add} {@link Update#op() operation}
     * adds to the entity attribute value. The parameter for this operation
     * specifies the amount added, or in the case of strings, the text to
     * append.</p>
     *
     * <p>Applies to: numeric, strings.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "id")
     * {@literal @Update}(attr = "price")
     * {@literal @Update}(attr = "priceChanges", op = Operation.Add, value = "1")
     * boolean changePrice(long productId, float newPrice);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * updated = products.changePrice(prodId, 5.99f);
     * </pre>
     */
    Add,

    /**
     * <p>The {@code Assign} {@link Update#op() operation}
     * assigns the entity attribute value to the value of the parameter
     * for this operation.</p>
     *
     * <p>Applies to: booleans, collections, numeric, strings, time.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "id")
     * {@literal @Update}(attr = "description", op = Operation.ASSIGN)
     * boolean setDescription(long productId, String newDescription);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * updated = products.setDescription(prodId, "A really good product.");
     * </pre>
     */
    Assign,

    /**
     * <p>The {@code Divide} {@link Update#op() operation}
     * divides the entity attribute value by the parameter for this operation.</p>
     *
     * <p>Applies to: numeric.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "id")
     * {@literal @Update}(attr = "price", op = Operation.Divide)
     * boolean putOnClearance(long productId, float divisor);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * updated = products.putOnClearance(prodId, 2.0f);
     * </pre>
     */
    Divide,

    /**
     * <p>The {@code Multiply} {@link Update#op() operation}
     * multiplies the entity attribute value by the parameter for this operation.</p>
     *
     * <p>Applies to: numeric.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "id")
     * {@literal @Update}(attr = "price", op = Operation.Multiply)
     * boolean adjustPrice(long productId, float rate);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * increased = products.adjustPrice(prodId1, 1.1f);
     * decreased = products.adjustPrice(prodId2, 0.9f);
     * </pre>
     */
    Multiply,

    /**
     * <p>The {@code Subtract} {@link Update#op() operation}
     * subtracts from the entity attribute value. The parameter for this operation
     * specifies the amount subtracted, or in the case of strings.</p>
     *
     * <p>Applies to: numeric.</p>
     *
     * <p>Example query:</p>
     *
     * <pre>
     * {@literal @Filter}(by = "id")
     * {@literal @Filter}(by = "remaining", op = Compare.GreaterThanEqual, param = "amount")
     * {@literal @Update}(attr = "remaining", op = Operation.Subtract, param = "amount")
     * boolean reserve(String id, {@literal @Param("amount")} int numToReserve);
     * </pre>
     *
     * <p>Example usage:</p>
     *
     * <pre>
     * reserved = products.reserve(prodId, 5);
     * </pre>
     */
    Subtract
}
