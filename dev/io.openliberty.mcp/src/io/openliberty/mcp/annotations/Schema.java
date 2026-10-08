/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.mcp.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mcpjava.server.tools.ToolArg;

/**
 * Augment the schema used for an object when it is part of a tool argument or structured output.
 * <p>
 * All tools have an input schema derived from the types of its tool arguments. Tools with structured content also have an output schema derived from its return type. Generally the
 * schema is created by looking at the structure of the types and how they are serialized by JSON-B. This annotation allows the created schema for a class to be
 * {@linkplain #value() overridden} or {@linkplain #description() augmented with a description}.
 * <p>
 * This annotation can be placed:
 * <ul>
 * <li>on a {@link Tool} method to affect its output schema
 * <li>on a {@link ToolArg} parameter to affect the input schema for that argument
 * <li>on a class to affect the schema generated for that class
 * <li>on a field, setter method or getter method to affect the schema generated for that property (overriding any schema that would have been generated from the type of that
 * property)
 * </ul>
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD, PARAMETER })
public @interface Schema {

    public final static String UNSET = "<<unset>>";

    /**
     * Override the schema used for the annotated element
     *
     * @return the JSON schema for the annotated element
     */
    String value() default UNSET;

    /**
     * Augment the schema used for the annotated element with a description
     *
     * @return the description
     */
    String description() default UNSET;

}