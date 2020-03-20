/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.cdi.services;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.microprofile.config14.cdi.Config14CDIExtension;
import com.ibm.wsspi.cdi.extension.WebSphereCDIExtension;

/**
 * The ConfigCDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
@Component(service = WebSphereCDIExtension.class, property = { "api.classes=org.eclipse.microprofile.config.inject.ConfigProperty;org.eclipse.microprofile.config.Config" }, immediate = true)
public class Config14CDIExtensionComponent extends Config14CDIExtension implements Extension, WebSphereCDIExtension {}
