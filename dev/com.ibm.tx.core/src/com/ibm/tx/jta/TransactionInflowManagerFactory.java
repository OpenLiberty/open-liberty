package com.ibm.tx.jta;
/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.lang.reflect.Method;

import com.ibm.tx.util.logging.FFDCFilter;

public class TransactionInflowManagerFactory
{
    private static TransactionInflowManager _tranInflowManager;

    public static synchronized TransactionInflowManager getTransactionInflowManager()
    {
        if(_tranInflowManager == null)
        {
            loadTranInflowManager();
        }

        return _tranInflowManager;        
    }

    private static void loadTranInflowManager()
    {
        try
        {
            final Class<?> clazz = Class.forName("com.ibm.tx.jta.impl.TransactionInflowManagerImpl");
            final Method m = clazz.getMethod("instance", (Class[])null);

            _tranInflowManager = (TransactionInflowManager) m.invoke(null, (Object[])null);
        }
        catch(Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.tx.jta.TransactionInflowManagerFactory.loadTranInflowManager", "40");
            _tranInflowManager = null;
        }
    }
}