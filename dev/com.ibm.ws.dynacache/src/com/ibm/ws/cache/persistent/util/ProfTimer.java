/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.util;

import java.util.concurrent.TimeUnit;

/**************************************************************************
 * Simple class for use in timing operations.
 *************************************************************************/
public class ProfTimer
{
    long start = 0;
    public ProfTimer()
    {
        reset();
    }

    public long elapsed()
    {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    public void reset()
    {
        start = System.nanoTime();
    }

}

