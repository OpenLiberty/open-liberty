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
package ejb;

import java.io.Serializable;
import java.rmi.RemoteException;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import shared.Business;
import shared.TestClass;
import shared.TestIDLIntf;
import shared.TestRemote;

@Stateless
@Remote
public class BusinessEjb implements Business {

    @Override
    public int takesInt(int i) {
        return i;
    }

    @Override
    public Integer takesInteger(Integer i) {
        return i;
    }

    @Override
    public String takesString(String s) {
        return s;
    }

    @Override
    public Object takesObject(Object o) {
        return o;
    }

    @Override
    public Serializable takesSerializable(Serializable s) {
        return s;
    }

    @Override
    public TestRemote takesEjbIface(TestRemote ejb) {
        return ejb;
    }

    @Override
    public java.rmi.Remote takesRemote(java.rmi.Remote r) {
        return r;
    }

    @Override
    public TestIDLIntf takesIDLEntity(TestIDLIntf e) throws RemoteException {
        return e;
    }

    @Override
    public TestClass takesTestClass(TestClass t) {
        return t;
    }

    @Override
    public int[] takesIntArray(int[] arr) {
        return arr;
    }

    @Override
    public Integer[] takesIntegerArray(Integer[] arr) {
        return arr;
    }

    @Override
    public String[] takesStringArray(String[] arr) {
        return arr;
    }

    @Override
    public Object[] takesObjectArray(Object[] arr) {
        return arr;
    }

    @Override
    public Serializable[] takesSerializableArray(Serializable[] arr) {
        return arr;
    }

    @Override
    public TestRemote[] takesEjbIfaceArray(TestRemote[] arr) {
        return arr;
    }

    @Override
    public java.rmi.Remote[] takesRemoteArray(java.rmi.Remote[] arr) {
        return arr;
    }

    @Override
    public TestIDLIntf[] takesIDLEntityArray(TestIDLIntf[] arr) {
        return arr;
    }

    @Override
    public TestClass[] takesTestClassArray(TestClass[] arr) {
        return arr;
    }

    @Override
    public Long[] takesTwoLongs(Long l1, Long l2) {
        return new Long[]{l1,l2};
    }
}
