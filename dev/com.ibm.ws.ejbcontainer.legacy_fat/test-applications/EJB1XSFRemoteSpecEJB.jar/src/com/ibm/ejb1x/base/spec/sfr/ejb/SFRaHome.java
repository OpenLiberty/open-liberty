/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.ejb;

/**
 * Home interface for Enterprise Bean: SFRa
 */
public interface SFRaHome extends javax.ejb.EJBHome {
    /**
     * Creates a default instance of Session Bean: SFRa
     */
    public SFRa create() throws javax.ejb.CreateException, java.rmi.RemoteException;

    /**
    */
    public SFRa create(boolean booleanValue, byte byteValue, char charValue, short shortValue, int intValue, long longValue, float floatValue, double doubleValue,
                       String stringValue) throws javax.ejb.CreateException, java.rmi.RemoteException;
}
