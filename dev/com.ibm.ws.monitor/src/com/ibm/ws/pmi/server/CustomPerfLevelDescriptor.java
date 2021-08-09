/*******************************************************************************
 * Copyright (c) 1997, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.pmi.server;

// Created this class to hold the module name in PerfLevelSpec.
// In custom PMI path[0] is not always the module name

// We couldn't add module name to the PerfLevelDescriptor to keep the backward compatibility
// between 5.0 and 4.0
public class CustomPerfLevelDescriptor extends PerfLevelDescriptor {
    private static final long serialVersionUID = -1008233654263885657L;
    private String moduleID;

    public CustomPerfLevelDescriptor(String[] path, int level, String moduleID) {
        super(path, level);
        this.moduleID = moduleID;
    }

    public String getModuleName() {
        return moduleID;
    }
}
