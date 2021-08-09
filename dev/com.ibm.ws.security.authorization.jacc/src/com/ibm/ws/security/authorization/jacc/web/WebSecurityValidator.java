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
package com.ibm.ws.security.authorization.jacc.web;

import java.security.Permission;

import javax.security.auth.Subject;
import javax.security.jacc.WebUserDataPermission;

/**
 ** this class is for enforcing the security constraints for Web servlet.
 ** since Servlet-3.x feature might not exist, all of servlet related code is located
 ** to the separate feature which only activated when servlet feature exists.
 **/
public interface WebSecurityValidator {
    boolean checkDataConstraints(String contextId, Object req, WebUserDataPermission webUDPermission);

    boolean checkResourceConstraints(String contextId, Object req, Permission webPerm, Subject subject);

}
