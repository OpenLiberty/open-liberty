/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author davidn
 *
 *	Runs periodically to update the current time.
 */
public class CachedTimerService extends TimerTask
{
	private AtomicLong	_currentTime = new AtomicLong(0);
	private Timer _timer = null; 

	public CachedTimerService (long granularity) {
		_timer = new Timer(true);
		_timer.schedule(this, 0, granularity);
	}
	
	public void run() {
		_currentTime.set(System.currentTimeMillis());
	}
	
	public void destroy() {
		cancel();
		_timer.cancel();
		_timer = null;
	}
	
	public long	getCurrentTime() {
		return (_currentTime.get());
	}
}

