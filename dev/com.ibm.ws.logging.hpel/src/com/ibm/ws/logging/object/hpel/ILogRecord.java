/*******************************************************************************
 * Copyright (c) 1999, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.object.hpel;

import java.util.ResourceBundle;
import java.util.logging.Level;

public interface ILogRecord {
	Level getLevel();
	String getLoggerName();
	String getMessage();
	long getMillis();
	Object[] getParameters();
	ResourceBundle getResourceBundle();
	String getResourceBundleName();
	long getSequenceNumber();
	String getSourceClassName();
	String getSourceMethodName();
	int getThreadID();
	Throwable getThrown();
	void setLevel(Level level);
	void setLoggerName(String name);
	void setMessage(String message);
	void setMillis(long millis);
	void setParameters(Object parameters[]);
	void setResourceBundle(ResourceBundle bundle);
	void setResourceBundleName(String name);
	void setSequenceNumber(long seq);
	void setSourceClassName(String sourceClassName);
	void setSourceMethodName(String sourceMethodName);
	void setThreadID(int threadID);
	void setThrown(Throwable thrown);
}