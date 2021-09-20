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
package com.ibm.ws.ejbcontainer.exception2x.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

/**
 * Remote interface for Enterprise Bean: SFRa
 */
public interface SFRa extends EJBObject {
    /**
     * Pre-defined keys string for directing when the EJBs should throw a RuntimeException.
     */
    static final int Normal = 0;
    static final int Create = 1;
    static final int PostCreate = 2;
    static final int Activate = 3;
    static final int Passivate = 4;
    static final int Load = 5;
    static final int Store = 6;
    static final int Remove = 7;

    /**
     * Get accessor for persistent attribute: pKey
     */
    public String getPKey() throws RemoteException;

    /**
     * Set accessor for persistent attribute: pKey
     */
    public void setPKey(String theKey) throws RemoteException;

    /**
     * Get accessor for persistent attribute: intValue
     */
    public int getIntValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: intValue
     */
    public void setIntValue(int intValue) throws RemoteException;

    /**
     * Get accessor for persistent attribute: stringValue
     */
    public String getStringValue() throws RemoteException;

    /**
     * Set accessor for persistent attribute: stringValue
     */
    public void setStringValue(String stringValue) throws RemoteException;

    /**
     * Echo the input string plus the key string
     */
    public String echoRequired(String str) throws RemoteException;
}