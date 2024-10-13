/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.common;

import java.security.Permission;

import javax.security.auth.Subject;

/**
 * Proxy interface to represent either a java.security.Policy or jakarta.security.jacc.Policy object
 */
public interface PolicyProxy {

    public void refresh();

    public void setPolicy();

    public boolean implies(String contextId, Subject subject, Permission permission);
}
