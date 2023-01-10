/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package jakarta.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.data.repository.Query;

/**
 * Annotates a repository method to provide the update items of
 * the <code>UPDATE</code> clause of a JPQL or Jakarta NoSQL text query.
 * The <code>@Where</code> annotation can be added to provide conditions.
 * Do not combine on a single method with {@link Delete &#64;Delete} or with
 * {@link Query &#64;Query}, which is can be more advanced way of providing
 * this information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Update {
    /**
     * One or more update items of the <code>UPDATE</code> clause of a
     * JPQL or Jakarta NoSQL text query that the annotated method should run.<p>
     *
     * This value can include named parameters or positional parameters,
     * but must not intermix both types on the same query.<p>
     *
     * Prefix all entity attributes with <code>o.</code> to refer to the
     * entity type being updated.<p>
     *
     * For example, with JPQL for relational databases,<p>
     *
     * <pre>
     * &#64;Update("o.location = :2, o.state = 'READY'")
     * &#64;Where("o.id = :1 AND o.state = 'PREPARING'")
     * boolean readyForPickup(long id, String location);
     * </pre>
     */
    String value();
}
