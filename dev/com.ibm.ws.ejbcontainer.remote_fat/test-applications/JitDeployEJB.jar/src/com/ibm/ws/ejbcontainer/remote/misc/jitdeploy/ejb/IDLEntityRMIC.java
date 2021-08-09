/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.misc.jitdeploy.ejb;

import java.rmi.RemoteException;

import javax.ejb.Remote;

import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.portable.IDLEntity;

/**
 * RMI Remote Stateless bean interface for testing IDLEntity parameters and
 * return types.
 **/
@Remote
public interface IDLEntityRMIC extends java.rmi.Remote {
    public void unique_IDLEntity_Method(CompletionStatus idlEntity) throws RemoteException;

    public void overloaded_IDLEntity_Method(CompletionStatus idlEntity) throws RemoteException;

    public void overloaded_IDLEntity_Method(int integer, CompletionStatus idlEntity) throws RemoteException;

    public CompletionStatus overloaded_IDLEntity_Method(CompletionStatus idlEntity, CompletionStatus idlEntity2) throws RemoteException;

    public void unique_IDLEntityArray_Method(CompletionStatus[] idlEntitys) throws RemoteException;

    public void overloaded_IDLEntityArray_Method(CompletionStatus[] idlEntitys) throws RemoteException;

    public void overloaded_IDLEntityArray_Method(int integer, CompletionStatus[] idlEntitys) throws RemoteException;

    public CompletionStatus[] overloaded_IDLEntityArray_Method(CompletionStatus[] idlEntitys, IDLEntity idlEntity) throws RemoteException;
}