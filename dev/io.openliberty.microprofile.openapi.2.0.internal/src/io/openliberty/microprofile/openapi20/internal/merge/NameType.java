/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.merge;

/**
 * The different types of name which can be referenced in an OpenAPI model
 */
public enum NameType {
    TAG(null),
    OPERATION_ID(null),
    CALLBACKS("callbacks"),
    EXAMPLES("examples"),
    HEADERS("headers"),
    LINKS("links"),
    PARAMETERS("parameters"),
    PATH_ITEMS("pathItems"),
    REQUEST_BODIES("requestBodies"),
    RESPONSES("responses"),
    SCHEMAS("schemas"),
    SECURITY_SCHEMES("securitySchemes"),
    PATHS(null),
    WEBHOOKS(null);

    private final String referencePrefix;

    /**
     * @param componentName the name of the component section where objects of this type are stored, or {@code null} if this type of object is not stored under components.
     */
    NameType(String componentName) {
        if (componentName != null) {
            this.referencePrefix = "#/components/" + componentName + "/";
        } else {
            this.referencePrefix = null;
        }
    }

    /**
     * The prefix to use for references to a component of this type
     *
     * @return the prefix, or {@code null} if this type of object is not stored in the components section
     */
    public String getReferencePrefix() {
        return referencePrefix;
    }

}