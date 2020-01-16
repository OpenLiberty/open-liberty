/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util.wlm;

import com.ibm.ws.sip.container.servlets.ext.SipServletMessageExt;
import com.ibm.ws.sip.container.util.Queueable;

/** One of these can be registered with the SIP
 * container to augment its processing of
 * incoming SIP messages.
 * @author dror yaffe
 *
 */
public interface QueueableTransformer {

	/** Given a Queueable that performs the container's
	 * work for an incoming message, this method returns another
	 * Queueable that may do additional work before
	 * and after the container's work (which
	 * will also be done as part of the
	 * returned Queueable's run method).
	 * This method is also given access to the message,
	 * and may remove proprietary headers from it.
	 *
	 * @param xn the SIP container's handle on the
	 * incoming message and the associated container data
	 * @param inner the Queueable whose run method
	 * does the container's work for the message
	 * @param sipMsg the SipServletMessage associated with the Queueable (can be null).
	 * this is helpul if one wish to add/remove custom headers
	 * @return the Queueable whose run method does both the
	 * container's work and the transformer's work
	 * for the message.
	 */
	public Queueable wrap(SipDialogContext xn, Queueable inner,SipServletMessageExt sipMsg);
	
}
