/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

import javax.ejb.EJBObject;

/**
 * Remote component interface for Stateful Session bean for testing Remove
 * methods.
 **/
public interface RemoveCMTEJBRemote extends EJBObject {
    /** Return the String value state of Stateful bean. **/
    public String getString() throws java.rmi.RemoteException;

    public String remove(String string) throws TestAppException, java.rmi.RemoteException;

    public String remove_RequiresNew(String string) throws java.rmi.RemoteException;

    public String remove_NotSupported(String string) throws TestAppException, java.rmi.RemoteException;

    /** Just another method to be different :-) **/
    public String howdy(String name) throws java.rmi.RemoteException;
}
