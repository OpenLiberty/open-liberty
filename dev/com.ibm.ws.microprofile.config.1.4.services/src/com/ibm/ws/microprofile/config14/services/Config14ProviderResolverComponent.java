/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config14.services;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.microprofile.config14.impl.Config14ProviderResolverImpl;

import io.openliberty.microprofile.config.internal.common.ConfigIntrospectionProvider;

@Component(service = { ConfigProviderResolver.class, ApplicationStateListener.class, ConfigIntrospectionProvider.class }, immediate = true)
public class Config14ProviderResolverComponent extends Config14ProviderResolverImpl {}