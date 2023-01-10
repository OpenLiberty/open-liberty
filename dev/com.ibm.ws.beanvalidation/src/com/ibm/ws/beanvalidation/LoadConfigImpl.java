/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.beanvalidation;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.beanvalidation.service.LoadConfig;

/**
 * Dummy component for ensuring all config has been loaded before OSGiBeanValidationImpl can become active.
 */
@Component(configurationPid = "com.ibm.ws.beanvalidation.service.LoadConfig",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LoadConfigImpl implements LoadConfig {

}
