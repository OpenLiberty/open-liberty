/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.converged.session;

import com.ibm.websphere.servlet.session.IBMApplicationSession;

public interface IBMApplicationSessionExt extends IBMApplicationSession {
    
//	TODO Libery Anat: can we remove it since we do not support HA ?
	/*
     * @return The Logical Server Name associated with the application session
     */
    //public String getLogicalServerName();
}
