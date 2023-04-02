/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.websphere.config;

/**
 *
 */
public class ConfigEvaluatorException extends Exception {

    private static final long serialVersionUID = 7497451013878508812L;

    /**
     * @param cause
     */
    public ConfigEvaluatorException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ConfigEvaluatorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public ConfigEvaluatorException(String message) {
        super(message);
    }

}
