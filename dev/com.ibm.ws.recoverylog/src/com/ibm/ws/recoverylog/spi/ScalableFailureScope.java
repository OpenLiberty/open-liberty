/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

public interface ScalableFailureScope extends FailureScope
{
    /**
     * Constant representing the UUID in the group properties object.
     */
    public static final String UUID = "SFS_UUID";

    /**
     * Constant representing the server short name in the group properties
     * object.
     */
    public static final String SERVER_SHORT = "SVR_SHORT";

    /**
     * Returns the server specific UUID for the FailureScope.
     */
    public String uuid();

    /**
     * Returns the stoken for the servent of this FailureScope, or
     * returns null if this FailureScope represents all servants.
     */
    public byte[] stoken();

    /**
     * Returns the server short name.
     */
    public String serverShortName();
}
