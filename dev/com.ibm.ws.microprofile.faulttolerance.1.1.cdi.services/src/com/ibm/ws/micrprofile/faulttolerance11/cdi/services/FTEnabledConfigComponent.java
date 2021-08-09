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
package com.ibm.ws.micrprofile.faulttolerance11.cdi.services;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.microprofile.faulttolerance.cdi.FTEnablementConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.impl.FTEnablementConfig11Impl;

@Component(service = FTEnablementConfig.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class FTEnabledConfigComponent extends FTEnablementConfig11Impl {}
