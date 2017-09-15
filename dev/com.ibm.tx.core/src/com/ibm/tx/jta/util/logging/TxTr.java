package com.ibm.tx.jta.util.logging;
/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.tx.util.logging.Tracer;

/**
 * TxTr is a java.util.logging based implemenation of Tracer that
 * can be used in a non-WAS environment. 
 * A single instance of this class is created by Tr; Tr then delegates
 * to this Tracer implementation.
 * 
 */
public class TxTr implements Tracer
{
    private static Logger _logger;
    private String SOURCE_METHOD;

    private static String TRACEFile = System.getProperty("com.ibm.tx.TraceFile", "trace.xml");
    private static String TRACELoggerName = System.getProperty("com.ibm.tx.TraceLoggerName");
    private static String TRACELoggerResourceBundle = System.getProperty("com.ibm.tx.TraceLoggerResourceBundle");

    public TxTr() throws Exception
    {
        this(TRACEFile);
    }

    public TxTr(String file) throws Exception
    {
        if (TRACELoggerName == null)
        {
            _logger = Logger.getAnonymousLogger();
        }
        else
        {
            _logger = Logger.getLogger(TRACELoggerName, TRACELoggerResourceBundle);
        }

        // Initialise trace to off.
        // The trace level is updated from config once the TM starts 
        _logger.setLevel(Level.OFF);
       
        _logger.addHandler(new FileHandler(file));
    }
    
    public void initTrace()
    {
        Level traceLevel=ConfigurationProviderManager.getConfigurationProvider().getTraceLevel();
        _logger.setLevel(traceLevel);
       
        // Update static TraceComponent guards.
        if (traceLevel.intValue() <= Level.FINE.intValue())
        {
            TxTraceComponent.svDebugEnabled=true;
            TxTraceComponent.svEntryEnabled=true;
            TxTraceComponent.svEventEnabled=true;
        }
    }

    public void debug(TraceComponent tc, String s)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }

    public void debug(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void debug(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void entry(TraceComponent tc, String s, Object o)
    {
        _logger.entering(((TxTraceComponent)tc).getClassName(), s, o);
    }

    public void entry(TraceComponent tc, String s, Object[] o)
    {
        _logger.entering(((TxTraceComponent)tc).getClassName(), s, o);
    }

    public void entry(TraceComponent tc, String s)
    {
        _logger.entering(((TxTraceComponent)tc).getClassName(), s);
    }

    public void error(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.SEVERE, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void error(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.SEVERE, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void error(TraceComponent tc, String s)
    {
        _logger.logp(Level.SEVERE, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }

    public void event(TraceComponent tc, String s)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }

    public void event(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void event(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void exit(TraceComponent tc, String s)
    {
        _logger.exiting(((TxTraceComponent)tc).getClassName(), s);
    }

    public void exit(TraceComponent tc, String s, Object o)
    {
        _logger.exiting(((TxTraceComponent)tc).getClassName(), s, o);
    }

    public void exit(TraceComponent tc, String s, Object[] o)
    {
        _logger.exiting(((TxTraceComponent)tc).getClassName(), s, o);
    }

    public void fatal(TraceComponent tc, String s)
    {
        _logger.logp(Level.SEVERE, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }

    public void fatal(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.SEVERE, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void fatal(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.SEVERE, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void info(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void info(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public TraceComponent register(Class cl, String traceGroup, String nlsFile)
    {
        return new TxTraceComponent(cl, traceGroup, nlsFile);
    }
    
    @Override
    public TraceComponent register(String s, String traceGroup, String nlsFile) {
       
       return new TxTraceComponent(s, traceGroup, nlsFile);
    }

    public void warning(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.WARNING, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void warning(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.WARNING, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void audit(TraceComponent tc, String s)
    {
        _logger.logp(Level.WARNING, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }

    public void audit(TraceComponent tc, String s, Object o)
    {
        _logger.logp(Level.WARNING, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void audit(TraceComponent tc, String s, Object[] o)
    {
        _logger.logp(Level.WARNING, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s, o);
    }

    public void info(TraceComponent tc, String s)
    {
        _logger.logp(Level.INFO, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }

    public void warning(TraceComponent tc, String s)
    {
        _logger.logp(Level.WARNING, ((TxTraceComponent)tc).getClassName(), SOURCE_METHOD, s);
    }
}
