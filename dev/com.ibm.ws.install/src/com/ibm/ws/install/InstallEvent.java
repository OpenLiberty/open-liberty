/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import java.util.EventObject;

/**
 * This abstract class is for Install Event notifications
 */
public abstract class InstallEvent extends EventObject {

    private static final long serialVersionUID = -1991865252974291860L;

    /**
     * Creates an Install event object
     *
     * @param notificationType Event notification type
     */
    public InstallEvent(String notificationType) {
        super(notificationType);
    }

}
