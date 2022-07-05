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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A list of additional entity classes that can be used with {@link Template}.
 * <code>Template</code> can also access entity classes from {@link Data}.<p>
 *
 * For example,<p>
 *
 * <pre>
 * &#64;Entities(MyEntity.class)
 * public class MyServlet extends HttpServlet {
 *     &#64;Inject
 *     Template template;
 *
 *     ...
 *     entity = template.find(MyEntity.class, primaryKey);
 * </pre>
 *
 * This annotation is repeatable, which allows multiple providers to be used.<p>
 *
 * This class is a CDI bean-defining annotation.
 */
@Repeatable(Entities.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entities {
    /**
     * Returns the name of the provider of backend data access for
     * these entity types.
     *
     * @return provider name.
     */
    String provider() default "DefaultDataStore";

    /**
     * Entity class(es) to make accessible via {@link Template}.
     *
     * @return one or more entity classes.
     */
    Class<?>[] value();

    /**
     * Enables multiple <code>Entities</code> annotations on the same class,
     * which is useful if entities from multiple providers are accessed via
     * the {@link Template}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface List {
        Entities[] value();
    }
}
