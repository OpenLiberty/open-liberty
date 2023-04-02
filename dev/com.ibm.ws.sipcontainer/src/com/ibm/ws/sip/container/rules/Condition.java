/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.rules;

import javax.servlet.sip.SipServletRequest;

/**
 * @author Amir Perlman, Jun 25, 2003
 *
 * A condition is either an operator or a logical connector that specify the
 * triggering rule under which a a servlet should be invoked. 
 */
public interface Condition
{
	/**
	 * Evaluate the condition for the given request. 
	 * @param request
	 * @return true if the condition is satisfied otherwise false. 
	 */
	public boolean evaluate(SipServletRequest request); 
}
