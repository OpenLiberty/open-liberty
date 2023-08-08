/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.internal.config.AggregatedConfiguration;
import com.ibm.ws.cdi.internal.config.CDI12AggregatedConfiguration;
import com.ibm.ws.cdi.internal.config.CDIConfiguration;

/**
 * DS for custom CDI 2.0 properties.
 */
@Component(name = "com.ibm.ws.cdi.impl.CDI20AggregatedConfigurationImpl", service = { CDIConfiguration.class,
                                                                                      AggregatedConfiguration.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class CDI20AggregatedConfigurationImpl extends CDI12AggregatedConfiguration {

}
