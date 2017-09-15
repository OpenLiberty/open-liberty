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
package com.ibm.ws.tx.util.logging;

import com.ibm.tx.util.logging.FFDCFilterer;
import com.ibm.ws.ffdc.FFDCFilter;

public class WASFFDCFilter implements FFDCFilterer
{
    public void processException(Throwable e, String s1, String s2, Object o)
    {
        FFDCFilter.processException(e, s1, s2, o);
    }

    public void processException(Throwable e, String s1, String s2)
    {
        FFDCFilter.processException(e, s1, s2);
    }

	public void processException(Throwable th, String sourceId, String probeId,
			Object[] objectArray)
	{
		FFDCFilter.processException(th, sourceId, probeId, objectArray);
	}

	public void processException(Throwable th, String sourceId, String probeId,
			Object callerThis, Object[] objectArray)
	{
		FFDCFilter.processException(th, sourceId, probeId, callerThis, objectArray);
	}
}
