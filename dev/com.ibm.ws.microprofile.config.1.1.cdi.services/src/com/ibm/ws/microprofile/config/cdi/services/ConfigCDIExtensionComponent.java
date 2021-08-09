/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.cdi.services;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.config.cdi.ConfigCDIExtension;

/**
 * The ConfigCDIExtension observes all the @ConfigProperty qualified InjectionPoints and ensures that a ConfigPropertyBean is created for each type.
 * It also registers the ConfigBean itself.
 */
@Component(service = WebSphereCDIExtension.class, property = { "api.classes=org.eclipse.microprofile.config.inject.ConfigProperty;org.eclipse.microprofile.config.Config" }, immediate = true)
public class ConfigCDIExtensionComponent extends ConfigCDIExtension implements Extension, WebSphereCDIExtension {}
