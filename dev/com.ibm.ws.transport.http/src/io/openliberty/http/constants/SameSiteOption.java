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
public enum SameSiteOption implements HttpEndpointOption {
    LAX("lax"),
    NONE("none"),
    STRICT("strict");

    String id;

    SameSiteOption(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object value() {
        // TODO Auto-generated method stub
        return null;
    }

}
