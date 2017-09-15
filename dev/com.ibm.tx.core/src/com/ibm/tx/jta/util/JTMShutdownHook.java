package com.ibm.tx.jta.util;
/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.tx.util.TMHelper;

/**
 * This singleton is registered as a shutdown hook in TxTMHelper such
 * that its run() method gets invoked as the JVM is going down. The
 * intention is to attempt to gracefully shutdown even when the JVM is killed.
 */
public class JTMShutdownHook extends Thread
{
    private static final TraceComponent tc = Tr.register(JTMShutdownHook.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static Thread _hook = new JTMShutdownHook("JTMShutdownHook");

    private JTMShutdownHook(String name)
    {
        super(name);
    }
    
    public static Thread instance()
    {
        return _hook;
    }
    
    public void run()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "run");

        try
        {
            // Use no arg shutdown so we can affect what happens via config
            TMHelper.shutdown();
        }
        catch(Exception e)
        {
            // Ah well. At least we tried. This is probably an NPE cos we're already shutdown.
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "run");
    }
}