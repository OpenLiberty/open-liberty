/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package shared;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

@javax.ejb.Remote
public interface Business extends Remote {
    int takesInt(int i) throws RemoteException;

    Integer takesInteger(Integer i) throws RemoteException;

    String takesString(String s) throws RemoteException;

    Object takesObject(Object o) throws RemoteException;

    Serializable takesSerializable(Serializable s) throws RemoteException;

    TestRemote takesEjbIface(TestRemote ejb) throws RemoteException;

    Remote takesRemote(Remote r) throws RemoteException;

    TestIDLIntf takesIDLEntity(TestIDLIntf e) throws RemoteException;

    TestClass takesTestClass(TestClass t) throws RemoteException;

    int[] takesIntArray(int[] arr) throws RemoteException;

    Integer[] takesIntegerArray(Integer[] arr) throws RemoteException;

    String[] takesStringArray(String[] arr) throws RemoteException;

    Object[] takesObjectArray(Object[] arr) throws RemoteException;

    Serializable[] takesSerializableArray(Serializable[] arr) throws RemoteException;

    TestRemote[] takesEjbIfaceArray(TestRemote[] arr) throws RemoteException;

    Remote[] takesRemoteArray(Remote[] arr) throws RemoteException;

    TestIDLIntf[] takesIDLEntityArray(TestIDLIntf[] arr) throws RemoteException;

    TestClass[] takesTestClassArray(TestClass[] arr) throws RemoteException;

    Long[] takesTwoLongs(Long l1, Long l2) throws RemoteException;
}
