/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a repository method to provide the conditional expression of
 * the <code>WHERE</code> clause of a JPQL or Jakarta NoSQL text query.
 * The <code>@Where</code> annotation on its own signifies a find operation.
 * The <code>@Where</code> annotation can be combined with the
 * {@link Update &#64;Update} or {@link Delete &#64;Delete} annotations to
 * signify a conditional update or delete operation.
 * Do not combine on a single method with {@link Query &#64;Query},
 * which is a more advanced way of providing this information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Where {
    /**
     * The conditional expression of the <code>WHERE</code> clause of a
     * JPQL or Jakarta NoSQL text query that the annotated method should run.<p>
     *
     * This value can include named parameters or positional parameters,
     * but must not intermix both types on the same query.<p>
     *
     * Prefix all entity attributes with <code>o.</code> to refer to the
     * entity type being queried.<p>
     *
     * For example, with JPQL for relational databases,<p>
     *
     * <pre>
     * &#64;Where("o.yearHired >= ?1 AND departureDate IS NULL")
     * Employee[] getNewHires(int minYearHired);
     *
     * &#64;Where("o.itemId = :id AND (o.normalPrice - o.currentPrice) / o.normalPrice >= :min")
     * Item findIfOnSale(&#64;Param("id") long idNum,
     *                   &#64;Param("min") float minDiscountPct);
     * </pre>
     */
    String value();
}
