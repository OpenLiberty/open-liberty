/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.request.probe;


import com.ibm.websphere.logging.hpel.LogRecordContext;

public class ThreadLocalStringExtension implements LogRecordContext.Extension {
	
	public ThreadLocalStringExtension() {}

	private ThreadLocal<String> threadLocalString = new ThreadLocal<String>();

	public void setValue(String string) {
		threadLocalString.set(string);
	}

	@Override
	public String getValue() {
		return threadLocalString.get();
	}
	
	public void remove() {
		 threadLocalString.remove();
	}
}

