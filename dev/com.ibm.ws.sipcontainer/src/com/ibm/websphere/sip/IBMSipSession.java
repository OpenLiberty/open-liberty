/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.websphere.sip;

import javax.servlet.sip.SipSession;

/**
 * An extension of the SipSession interface. 
 * SipSession has related SipSession when a request which initiated that
 * session contains a "Join" or "Replace" header. In this case, it is useful for  
 * the application to be able to access the SipSession which is mentioned in those "Join" 
 * or "Replace" headers (the related SipSession).
 * 
 * @author Anat Fradin
 * @ibm-api
 */
public interface IBMSipSession extends SipSession {
	
	/**
	 * @return related SipSession, or null when no related sessions exist
	 */
	public SipSession getRelatedSipSession();
}
