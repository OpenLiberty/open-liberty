/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.constants;

/**
 *
 */
public enum CompressionOption implements HttpEndpointOption {
    TYPES("types", "\"text/*, application/javascript\""),
    SERVER_PREFERRED_ALGORITHM("serverPreferredAlgorithm", "none");

    String id;
    Object defaultValue;

    /**
     *
     */
    CompressionOption(String id, String defaultValue) {
        this.id = id;
        this.defaultValue = defaultValue;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Object value() {
        return value;
    }

}
