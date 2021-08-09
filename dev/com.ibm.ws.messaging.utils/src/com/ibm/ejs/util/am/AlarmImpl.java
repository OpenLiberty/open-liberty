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
package com.ibm.ejs.util.am;

import java.util.concurrent.ScheduledFuture;

public class AlarmImpl implements Alarm {

	private ScheduledFuture future = null;

	AlarmImpl(ScheduledFuture future) {
		this.future = future;
	}

	public void cancel() {
		future.cancel(true);
	}
}
