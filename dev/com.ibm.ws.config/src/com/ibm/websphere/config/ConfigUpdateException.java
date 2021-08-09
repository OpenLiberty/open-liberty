/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
 *
 */
public class ConfigUpdateException extends Exception {

    /**  */
    private static final long serialVersionUID = -4540509541009993L;

    /**
     * @param formatMessage
     */
    public ConfigUpdateException(String formatMessage) {
        super(formatMessage);
    }

    /**
     * 
     * @param ex
     */
    public ConfigUpdateException(Exception ex) {
        super(ex.getMessage(), ex);
    }

}
