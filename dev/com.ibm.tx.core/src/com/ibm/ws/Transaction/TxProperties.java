package com.ibm.ws.Transaction;

/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * <p> This class is private to WAS.
 * Any use of this class outside the WAS Express/ND codebase
 * is not supported.
 * 
 */
public class TxProperties
{
    public final static String LTC_KEY = "ltc.always.required";
    public final static String NATIVE_KEY = "native.contexts.used";
    public final static String SINGLE_KEY = "single.process";
    public final static String JTA2INTEROP_KEY = "jta2.interop.supported";
    public final static boolean isZOS = false; // False on Liberty

    private final static Properties props = new Properties();

    static
    {
        final ClassLoader cl = TxProperties.class.getClassLoader();;
        final InputStream stream = cl.getResourceAsStream("transaction.properties");

        try
        {
            if (stream != null)
                props.load(stream);
        } catch (IOException ioe)
        {
            throw new IllegalStateException(ioe.getMessage());
        }
    }

    public final static boolean LTC_ALWAYS_REQUIRED =
                    Boolean.valueOf(props.getProperty(LTC_KEY, (isZOS ? "true" : "false"))).booleanValue();

    public final static boolean NATIVE_CONTEXTS_USED =
                    Boolean.valueOf(props.getProperty(NATIVE_KEY, (isZOS ? "true" : "false"))).booleanValue();

    // Indicator that WebSphere is running in a single process or multiple servant processes
    public final static boolean SINGLE_PROCESS =
                    Boolean.valueOf(props.getProperty(SINGLE_KEY, (isZOS ? "false" : "true"))).booleanValue();

    // Indicator that WebSphere supports JTA2 private interop protocol
    public final static boolean JTA2_INTEROP_SUPPORTED =
                    Boolean.valueOf(props.getProperty(JTA2INTEROP_KEY, (isZOS ? "false" : "true"))).booleanValue();

    // Indicator that WebSphere supports SHAREABLE LTC containment (WAS6.1 compatibility)
    // ... set via custom property in TxServiceImpl
    public static boolean SHAREABLE_LTC = false;
}
