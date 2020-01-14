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
package com.ibm.ws.sip.container.util.wlm;

/**
 * The WLMDailogFailoverListener is an extention point for WLM handle Dailog failover.
 * @author dror
 *
 */
public interface WLMDialogFailoverListner {
	/**
	 * After the dailog is activated on the container because of failover. 
	 * the container will call WLM component.
	 * one example of use can be to set new DialogAux. (as the DialogAux that was 
	 * associated with this dialog was not failed over to the new container
	 * @param msgContext
	 */
	public void doFailover(SipDialogContext msgContext);
}
