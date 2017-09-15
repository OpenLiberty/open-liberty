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
package com.ibm.ws.tx.util.logging;

import com.ibm.ejs.ras.TraceComponent;

public class WASTraceComponent implements com.ibm.tx.util.logging.TraceComponent
{
    private TraceComponent _tc;

    public WASTraceComponent(TraceComponent tc)
    {
        _tc = tc;
        
    }

    public Object getData()
    {
        return _tc;
    }

    public boolean isDebugEnabled()
    {
        return TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled();
    }

    public boolean isEntryEnabled()
    {
        return TraceComponent.isAnyTracingEnabled() && _tc.isEntryEnabled();
    }

    public boolean isEventEnabled()
    {
        return TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled();
    }

    public boolean isWarningEnabled()
    {
        return TraceComponent.isAnyTracingEnabled() && _tc.isWarningEnabled();
    }

	public void setDebugEnabled(boolean arg0)
	{
		// Not required in WAS version
		
	}

	public void setEntryEnabled(boolean arg0)
	{
		// Not required in WAS version
		
	}

	public void setEventEnabled(boolean arg0)
	{
		// Not required in WAS version
		
	}

	public void setWarningEnabled(boolean arg0)
	{
		// Not required in WAS version
		
	}
}
