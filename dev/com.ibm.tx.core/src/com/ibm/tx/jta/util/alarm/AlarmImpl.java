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
package com.ibm.tx.jta.util.alarm;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.ibm.tx.util.alarm.Alarm;

public class AlarmImpl implements Alarm
{
	private ScheduledFuture _future;
	private ThreadPoolExecutor _executor;
	
	public AlarmImpl(ScheduledFuture future, ThreadPoolExecutor executor)
	{
		_future = future;
		_executor = executor;
	}

	public boolean cancel()
	{
		if (_future.cancel(false))
		{
			_executor.purge();

            return true;
        }

		return false;
	}
}
