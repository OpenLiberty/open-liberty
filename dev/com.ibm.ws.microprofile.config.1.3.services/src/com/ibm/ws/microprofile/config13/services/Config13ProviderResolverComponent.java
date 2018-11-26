/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.services;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.microprofile.config13.archaius.Config13ProviderResolverImpl;

@Component(service = { ConfigProviderResolver.class, ApplicationStateListener.class }, property = { "service.vendor=IBM" }, immediate = true)
public class Config13ProviderResolverComponent extends Config13ProviderResolverImpl {}