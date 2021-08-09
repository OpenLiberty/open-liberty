/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.variables.ConfigVariable;

public class ConfigMergeException extends ConfigUpdateException {

    /**  */
    private static final long serialVersionUID = -4706030558184048028L;

    /**
     * @param ex
     */
    public ConfigMergeException(Exception ex) {
        super(ex);
    }

    /**
     * @param in
     */
    public ConfigMergeException(ConfigID id) {
        super("The configuration element " + id + " can not be merged.");
    }

    public ConfigMergeException(ConfigVariable var) {
        super("The configuration variable " + var.getName() + " can not be merged.");
    }

}