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
package com.ibm.tx.jta.util.logging;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.tx.util.logging.FFDCFilterer;

public class TxFFDCFilter implements FFDCFilterer
{
    private Logger _logger;

    private static String FFDCFile = System.getProperty("com.ibm.tx.FFDCFile", "ffdc.xml");
    private static String FFDCLoggerName = System.getProperty("com.ibm.tx.FFDCLoggerName");
    private static String FFDCLoggerResourceBundle = System.getProperty("com.ibm.tx.FFDCLoggerResourceBundle");

    public TxFFDCFilter() throws Exception
    {
        this(FFDCFile);
    }

    public TxFFDCFilter(String file) throws Exception
    {
        if (FFDCLoggerName == null)
        {
            _logger = Logger.getAnonymousLogger();
        }
        else
        {
            _logger = Logger.getLogger(FFDCLoggerName, FFDCLoggerResourceBundle);
        }

        _logger.setLevel(Level.ALL);

        _logger.addHandler(new FileHandler(file));
    }

    public void processException(Throwable e, String s1, String s2, Object o)
    {
        _logger.logp(Level.SEVERE, s1, s2, o.toString(), e);
    }

    public void processException(Throwable e, String s1, String s2)
    {
        _logger.logp(Level.SEVERE, s1, s2, "", e);
    }

    public void processException(Throwable th, String sourceId, String probeId,
			Object[] objectArray)
	{
		_logger.logp(Level.SEVERE, sourceId, probeId, objectArray.toString(), th);
	}

	public void processException(Throwable th, String sourceId, String probeId,
			Object callerThis, Object[] objectArray)
	{
		_logger.logp(Level.SEVERE, callerThis + " " + sourceId, probeId, objectArray.toString(), th);
	}
}
