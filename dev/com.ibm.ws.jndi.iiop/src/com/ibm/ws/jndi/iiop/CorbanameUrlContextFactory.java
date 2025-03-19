/*******************************************************************************
 * Copyright (c) 2017,2024 IBM Corporation and others.
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
package com.ibm.ws.jndi.iiop;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;

@Component(configurationPolicy=IGNORE,property={"service.vendor=ibm","osgi.jndi.url.scheme=corbaname"})
public class CorbanameUrlContextFactory extends UrlContextFactory implements ObjectFactory, ApplicationRecycleComponent {}
