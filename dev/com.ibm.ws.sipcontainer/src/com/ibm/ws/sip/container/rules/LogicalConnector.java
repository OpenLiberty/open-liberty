/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.rules;

import javax.servlet.sip.SipServletRequest;

/**
 * @author Amir Perlman, Jun 25, 2003
 *
 * Base class for Logical Connector conditions. 
 */
public abstract class LogicalConnector implements Condition
{
	/**
	 * @see com.ibm.ws.sip.container.rules.Condition#evaluate(javax.servlet.sip.SipServletRequest)
	 */
	public abstract boolean evaluate(SipServletRequest request);
}
