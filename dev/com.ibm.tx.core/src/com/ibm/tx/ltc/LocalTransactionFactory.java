/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.ltc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.ws.LocalTransaction.LocalTransactionCurrent;
import com.ibm.ws.Transaction.UOWCurrent;

public class LocalTransactionFactory
{
    private static LocalTransactionCurrent _localTranCurrent;
    

    public static  LocalTransactionCurrent getLocalTransactionCurrent()
    {
        if(_localTranCurrent == null)
        {
            loadImplementation();
        }

        return _localTranCurrent;        
    }

    public static  UOWCurrent getUOWCurrent()
    {
        if(_localTranCurrent == null)
        {
            loadImplementation();
        }

        return (UOWCurrent) _localTranCurrent;        
    }

    // used by for example WAS LTCFactory to set it's Current here to be consistent
    public static void setLocalTransactionCurrent(LocalTransactionCurrent  localTranCurrent)
    {
        _localTranCurrent = localTranCurrent;
    }


    private static void loadImplementation()
    {
        final Class<?> clazz;
        final Method m;

        try
        {
            clazz = Class.forName("com.ibm.tx.ltc.impl.LocalTranCurrentSet");
            m = clazz.getMethod("instance", (Class[])null);

            _localTranCurrent = (LocalTransactionCurrent  ) m.invoke(null, (Object[])null);
        }
        catch(SecurityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(ClassNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(NoSuchMethodException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(InvocationTargetException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}