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
package com.ibm.ws.security.authorization.jacc.ejb;

import java.security.Permission;
import java.util.List;

import javax.security.auth.Subject;

/**
 ** this class is for enforcing the security constraints for EJB.
 ** since EJB feature might not exist, all of EJB related code is located
 ** to the separate feature which only activated when ejb feature exists.
 **/
public interface EJBSecurityValidator {
    boolean checkResourceConstraints(String contextId, List<Object> methodParameters, Object bean, Permission ejbPerm, Subject subject);
}
