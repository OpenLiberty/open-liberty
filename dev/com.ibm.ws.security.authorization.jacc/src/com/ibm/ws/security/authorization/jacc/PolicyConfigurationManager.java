/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;

public interface PolicyConfigurationManager {

    void setEJBSecurityPropagator(EJBSecurityPropagator esp);

    void initialize(PolicyProxy policyProxy, PolicyConfigurationFactory pcf);

    void linkConfiguration(String appName, PolicyConfiguration pc) throws PolicyContextException;

    void addModule(String appName, String contextId);

    boolean containModule(String appName, String contextId);

    void removeModule(String appName, String contextId);

    void addEJB(String appName, String contextId);

    public boolean isApplicationRunning(String appName);
}
