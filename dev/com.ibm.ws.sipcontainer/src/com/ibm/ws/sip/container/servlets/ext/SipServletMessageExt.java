/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets.ext;

import javax.servlet.sip.SipServletMessage;

/**
 * This extention will alow the container to expose container data on the SipServletMessage
 * which is not included by the JSR 
 * @author dror
 *
 */
public interface SipServletMessageExt extends SipServletMessage {
	/**
	 * the time <msec> when the request arrived. can return -1 after failover occur
	 * @return
	 */
	public long getArrivedTime();
}
