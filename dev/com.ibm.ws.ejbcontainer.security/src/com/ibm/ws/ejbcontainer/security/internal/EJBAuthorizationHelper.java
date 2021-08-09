/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.security.internal;

import javax.security.auth.Subject;

import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBRequestData;

/**
 * Encapsulate jacc related methods which are consumed by EJBSecurityCollaborator.
 */
public interface EJBAuthorizationHelper {

    void authorizeEJB(EJBRequestData request, Subject subject) throws EJBAccessDeniedException;

    boolean isCallerInRole(EJBComponentMetaData cmd, EJBRequestData request, String roleName, String roleLink, Subject subject);
}
