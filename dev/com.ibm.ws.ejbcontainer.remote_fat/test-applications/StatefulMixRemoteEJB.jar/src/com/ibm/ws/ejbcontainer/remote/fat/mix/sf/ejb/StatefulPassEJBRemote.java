/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb;

import java.rmi.RemoteException;

import javax.ejb.EJBObject;

public interface StatefulPassEJBRemote extends EJBObject {
    public void setStringValue(String value) throws RemoteException;

    public String getStringValue() throws RemoteException;

    public void setIntegerValue(Integer value) throws RemoteException;

    public Integer getIntegerValue() throws RemoteException;

    public void setSerObjValue(SerObj value) throws RemoteException;

    public SerObj getSerObjValue() throws RemoteException;

    public void setSerObj2Value(SerObj2 value) throws RemoteException;

    public SerObj2 getSerObj2Value() throws RemoteException;

/*
 * public void checkMySerObjStart() throws RemoteException;
 * public void checkMySerObjEnd() throws RemoteException;
 */
    public int getPassivateCount() throws RemoteException;

    public int getActivateCount() throws RemoteException;

    public void checkTimerStart() throws RemoteException;

    public void checkTimerEnd() throws RemoteException;
}
