/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.util.logging;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

public class FFDCFilter
{
    private static FFDCFilterer f;

    static
    {
        String FFDCFiltererClass = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("com.ibm.tx.ffdcfilterer");
            }
        });

        if (FFDCFiltererClass == null)
        {
            FFDCFiltererClass = "com.ibm.ws.tx.util.logging.WASFFDCFilter";
        }

        try
        {
            final String finalFFDCFiltererClass = FFDCFiltererClass;
            Class<?> FFDCFiltererClassObj = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                @Override
                public Class<?> run() throws ClassNotFoundException {
                    return Class.forName(finalFFDCFiltererClass);
                }
            });
            f = (FFDCFilterer) FFDCFiltererClassObj.newInstance();
        } catch (Exception e)
        {
            try
            {
                Class<?> FFDCFiltererClassObj = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                    @Override
                    public Class<?> run() throws ClassNotFoundException {
                        return Class.forName("com.ibm.tx.jta.util.logging.TxFFDCFilter");
                    }
                });
                f = (FFDCFilterer) FFDCFiltererClassObj.newInstance();
            } catch (Exception e1)
            {
                f = null;
                e1.printStackTrace();
            }
        }

        if (f == null)
        {
            f = new FFDCFilterer()
            {
                @Override
                public void processException(Throwable e, String s1, String s2, Object o)
                {
                    processException(e, s1, s2, o, null);
                }

                @Override
                public void processException(Throwable e, String s1, String s2)
                {
                    processException(e, s1, s2, null, null);
                }

                @Override
                public void processException(Throwable th, String sourceId,
                                             String probeId, Object[] objectArray)
                {
                    processException(th, sourceId, probeId, null, objectArray);
                }

                @Override
                public void processException(Throwable th, String sourceId,
                                             String probeId, Object callerThis, Object[] objectArray)
                {
                    System.out.println("Method: " + sourceId);
                    System.out.println("Probe id: " + probeId);
                    th.printStackTrace(System.out);

                    if (callerThis != null)
                    {
                        System.out.println(callerThis);
                    }

                    if (objectArray != null)
                    {
                        for (Object o : objectArray)
                        {
                            System.out.println(o);
                        }
                    }
                }
            };
        }
    }

    public static void processException(Throwable e, String s1, String s2, Object o)
    {
        f.processException(e, s1, s2, o);
    }

    public static void processException(Throwable e, String s1, String s2)
    {
        f.processException(e, s1, s2);
    }

    public static void processException(Throwable th, String sourceId, String probeId, Object[] objectArray)
    {
        f.processException(th, sourceId, probeId, objectArray);
    }

    public static void processException(Throwable th, String sourceId, String probeId, Object callerThis, Object[] objectArray)
    {
        f.processException(th, sourceId, probeId, callerThis, objectArray);
    }
}