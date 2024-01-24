/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.concurrent.internal;

import javax.enterprise.concurrent.ContextService;

import org.eclipse.microprofile.context.ThreadContext;

import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Captures and propagates thread context.
 * This class implements the Jakarta/Java EE ContextService as well as MicroProfile ThreadContext.
 *
 * This is an OSGi Component, defined in the Host bundle (com.ibm.ws.concurrent)
 *
 * @Component(name = "com.ibm.ws.context.service",
 *                 configurationPolicy = ConfigurationPolicy.REQUIRE,
 *                 service = { ResourceFactory.class, ContextService.class, ThreadContext.class, WSContextService.class, ApplicationRecycleComponent.class },
 *                 property = { "creates.objectClass=javax.enterprise.concurrent.ContextService",
 *                 "creates.objectClass=org.eclipse.microprofile.context.ThreadContext" })
 */
public class ContextServiceImpl extends ContextServiceBase implements ContextService, //
                ResourceFactory, ThreadContext, WSContextService, ApplicationRecycleComponent {

}