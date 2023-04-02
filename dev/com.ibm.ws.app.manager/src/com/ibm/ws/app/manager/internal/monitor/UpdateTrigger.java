/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.app.manager.internal.monitor;

public enum UpdateTrigger {
    POLLED, MBEAN, DISABLED;

    /**
     * @param val
     * @return
     */
    public static UpdateTrigger get(String val) {
        if (DISABLED.toString().equalsIgnoreCase(val)) {
            return DISABLED;
        } else if (MBEAN.toString().equalsIgnoreCase(val)) {
            return MBEAN;
        }
        return POLLED;
    }
}