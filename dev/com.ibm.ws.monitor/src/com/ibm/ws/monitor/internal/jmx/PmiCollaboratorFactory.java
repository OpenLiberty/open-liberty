/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.monitor.internal.jmx;

public class PmiCollaboratorFactory {
    // return an instance of PmiCollaboratorMBean based on serverType
    public static PmiCollaboratorMBean getPmiCollaborator(String serverType) {
        // return PmiCollaborator for now. Later may have multiple implementations
        // return new PmiCollaborator();
        return PmiCollaborator.getSingletonInstance();
    }

    public static PmiCollaboratorMBean getPmiCollaborator() {
        return PmiCollaborator.getSingletonInstance();
    }
}
