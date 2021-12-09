/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.mdb.jms.mix.ejb;

import javax.ejb.EJBLocalObject;

/**
 * Local interface for Enterprise Bean: SFBean
 */
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