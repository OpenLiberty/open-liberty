/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Tie;
import javax.rmi.CORBA.UtilDelegate;
import javax.rmi.CORBA.ValueHandler;

import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public class TestUtilDelegateImpl
                implements UtilDelegate
{
    private static String svMethodName;

    public static void initialize()
    {
        System.setProperty("javax.rmi.CORBA.UtilClass", TestUtilDelegateImpl.class.getName());
    }

    public static String resetMethodName()
    {
        String methodName = svMethodName;
        svMethodName = null;
        return methodName;
    }

    public RemoteException mapSystemException(SystemException ex)
    {
        throw new UnsupportedOperationException();
    }

    public void writeAny(OutputStream out, Object obj)
    {
        svMethodName = "writeAny";
        out.write_any(null);
    }

    public Object readAny(InputStream in)
    {
        svMethodName = "readAny";
        return in.read_any();
    }

    public void writeRemoteObject(OutputStream out, Object obj)
    {
        svMethodName = "writeRemoteObject";
        out.write_Object((org.omg.CORBA.Object) obj);
    }

    public void writeAbstractObject(OutputStream out, Object obj)
    {
        svMethodName = "writeAbstractObject";
        ((org.omg.CORBA_2_3.portable.OutputStream) out).write_abstract_interface(obj);
    }

    public void registerTarget(Tie tie, Remote target)
    {
        throw new UnsupportedOperationException();
    }

    public void unexportObject(Remote target)
    {
        throw new UnsupportedOperationException();
    }

    public Tie getTie(Remote target)
    {
        throw new UnsupportedOperationException();
    }

    public ValueHandler createValueHandler()
    {
        throw new UnsupportedOperationException();
    }

    public String getCodebase(@SuppressWarnings("rawtypes") Class clz)
    {
        throw new UnsupportedOperationException();
    }

    public Class<?> loadClass(String className, String remoteCodebase, ClassLoader loader)
    {
        throw new UnsupportedOperationException();
    }

    public boolean isLocal(Stub stub)
    {
        return false;
    }

    public RemoteException wrapException(Throwable obj)
    {
        throw new UnsupportedOperationException();
    }

    public Object copyObject(Object obj, ORB orb)
    {
        throw new UnsupportedOperationException();
    }

    public Object[] copyObjects(Object[] obj, ORB orb)
    {
        throw new UnsupportedOperationException();
    }
}
