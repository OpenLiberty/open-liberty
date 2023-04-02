/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.ejb20;

import javax.ejb.EJBLocalObject;

public interface SFLocal extends EJBLocalObject {
    /**
     * Get accessor for persistent attribute: intValue
     */
    public int getIntValue();

    /**
     * Set accessor for persistent attribute: intValue
     */
    public void setIntValue(int newIntValue);

    public void incrementInt();

    /**
     * Business method
     */
    public String method1(String arg1);
}