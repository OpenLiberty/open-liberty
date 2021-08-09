/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ejb.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.application.handler.ApplicationTypeSupported;

@Component(service = ApplicationTypeSupported.class,
           property = { "service.vendor=IBM", "type:String=ejb" })
public class EJBApplicationTypeSupported implements ApplicationTypeSupported {}
