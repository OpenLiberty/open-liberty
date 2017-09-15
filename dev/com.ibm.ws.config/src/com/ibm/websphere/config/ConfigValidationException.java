/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.config;

/**
 * An exception representing an error occurred while parsing configuration
 * documents.
 */
public class ConfigValidationException extends Exception {
    private static final long serialVersionUID = -8341749732382155484L;
    public String docLocation = "";

    public ConfigValidationException() {
        super();
    }

    public ConfigValidationException(String message) {
        super(message);
    }

    public ConfigValidationException(String message, String doc) {
        super(message);
        this.docLocation = doc;
    }

}
