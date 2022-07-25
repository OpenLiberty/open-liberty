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
package io.openliberty.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the ability to choose the type and columns that are returned by find operations.
 * Do not combine on a single method with {@link Query @Query}, which is a more advanced way of providing this information.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Select {
    /**
     * Serves as a marker for automatic detection.
     */
    public static final class AutoDetect {
    }

    /**
     * Requests that only unique values of an entity attribute be returned.
     * When specifying <code>distinct</code> the {@link #value} should be a single entity attribute.
     * The default value is <code>false</code>.
     *
     * @return whether only unique values of an entity attribute should be returned.
     */
    boolean distinct() default false;

    /**
     * Specifies a return type for query results.
     * Instances of this type are constructed with the entity attributes
     * selected in the order that is specified by {@link #value}.
     * The default value is the marker class for automatic detection based on the method return type and entity type.
     *
     * @return the return type for query results, or {@link AutoDetect}.
     */
    Class<?> type() default AutoDetect.class;

    /**
     * Limits the entity attributes to the names listed.
     * The order in which the are listed is honored, enabling the attributes to be supplied as
     * constructor arguments per the {@link #type}.
     * When specified as empty (the default), all entity attributes are retrieved.
     *
     * @return names of entity attributes to select, or empty list for all.
     */
    String[] value() default {};
}
