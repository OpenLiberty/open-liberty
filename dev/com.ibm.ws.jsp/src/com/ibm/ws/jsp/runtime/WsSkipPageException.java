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
/*
 * Created on Nov 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ibm.ws.jsp.runtime;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.SkipPageException;

/**
 * @author todd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WsSkipPageException extends SkipPageException {
	
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3977862860699546417L;
	private static Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.runtime.WsSkipPageException";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}       

	public WsSkipPageException() {
		super();
		logger.logp(Level.FINE, CLASS_NAME, "WsSkipPageException", "DefaultConstructor");
		
	}
	public WsSkipPageException(String arg0) {
		super(arg0);
		logger.logp(Level.FINE, CLASS_NAME, "WsSkipPageException", "Message: [" + arg0 +"]");
	}
	public WsSkipPageException(String arg0, Throwable arg1) {
		super(arg0, arg1);
		logger.logp(Level.FINE, CLASS_NAME, "WsSkipPageException", "Message: [" + arg0 +"] RootCause:", arg1);
	}
	public WsSkipPageException(Throwable arg0) {
		super(arg0);
		logger.logp(Level.FINE, CLASS_NAME, "WsSkipPageException", "RootCause:", arg0);
	}

	public void printStackTraceIfTraceEnabled() {
		logger.logp(Level.FINE, CLASS_NAME, "printStackTraceIfTraceEnabled", "Exception occured:", this);
	}


}
