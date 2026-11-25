/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola.jca;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Class to allow use of Tr.debug for debug statements without deprecating existing debugMode flag.
 */
public class TraceUtil {
	
	/**
	 * TraceComponent registered by owning class.
	 */
	private TraceComponent tc = null;
	
	/**
	 * Constructor.
	 * @param tc TraceComponent registered with RAS
	 */
	public TraceUtil(TraceComponent tc) {
		this.tc = tc;
	}
	
	/**
	 * Print a trace message using Tr.debug.
	 * If debugMode == true, also send the message to system out.
	 * 
	 * @param msg
	 * @param debugMode
	 */
	public void debug(String msg, boolean debugMode) {
		if (tc != null && tc.isDebugEnabled()) {
			Tr.debug(tc, msg);
		}
		if (debugMode) {
			System.out.println(msg);
		}
	}

}
