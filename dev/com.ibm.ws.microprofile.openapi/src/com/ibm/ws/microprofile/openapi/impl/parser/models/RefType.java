/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.parser.models;

/**
 * Created by gracekarina on 16/06/17.
 */
public enum RefType {
    COMPONENTS("#/components/"),
    PATH("#/paths/");

    private final String internalPrefix;

    RefType(final String prefix) {
        this.internalPrefix = prefix;
    }

    /**
     * The prefix in an internal reference of this type.
     */
    public String getInternalPrefix() {
        return internalPrefix;
    }
}