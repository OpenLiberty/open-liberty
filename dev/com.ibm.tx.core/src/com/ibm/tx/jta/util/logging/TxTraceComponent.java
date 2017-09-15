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

import com.ibm.tx.util.logging.TraceComponent;

public class TxTraceComponent implements TraceComponent
{
    private Class _class;
    private String _name;
    private String _traceGroup;
    private String _nlsFile;

    /**
     * Trace guard fields.
     * Even though the interface defines getters and setters for the following
     * fields, the TxTraceComponent implementation treats these as
     * class fields shared by each TraceComponent instance.
     * TxTr initialises these based on the ConfigurationProvider traceLevel.
     */
    public static boolean svEntryEnabled = false;
    public static boolean svEventEnabled = false;
    public static boolean svDebugEnabled = false;
    public static boolean svWarningEnabled = true;

    public TxTraceComponent(Class cl, String traceGroup, String nlsFile)
    {
        _class = cl;
        _traceGroup = traceGroup;
        _nlsFile = nlsFile;
    }

    public TxTraceComponent(String s, String traceGroup, String nlsFile) {
       _name = s;
       _traceGroup = traceGroup;
       _nlsFile = nlsFile;
    }

   public boolean isDebugEnabled()
    {
        return svDebugEnabled;
    }

    public boolean isEntryEnabled()
    {
        return svEntryEnabled;
    }

    public boolean isEventEnabled()
    {
        return svEventEnabled;
    }

    public boolean isWarningEnabled()
    {
        return svWarningEnabled;
    }

    public Object getData()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public String getClassName()
    {
        return _class != null ?_class.getName() : _name;
    }

	public void setDebugEnabled(boolean enabled)
	{
		svDebugEnabled = enabled;
	}

	public void setEntryEnabled(boolean enabled)
	{
		svEntryEnabled = enabled;
	}

	public void setEventEnabled(boolean enabled)
	{
		svEventEnabled = enabled;
	}

	public void setWarningEnabled(boolean enabled)
	{
		svWarningEnabled = enabled;
	}
}
