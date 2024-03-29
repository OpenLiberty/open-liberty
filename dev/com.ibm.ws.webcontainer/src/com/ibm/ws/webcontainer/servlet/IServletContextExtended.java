/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.servlet;

import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorHelper;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *  RTC 160610. Contains methods moved from IServletContext which should not be spi.
 */
public interface IServletContextExtended extends IServletContext {

    public IHttpSessionContext getSessionContext();
    
    public ICollaboratorHelper getCollaboratorHelper();

}
