/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.metrics10.impl;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

/**
 * A map of shared, named metric registries.
 */
@Component(service = SharedMetricRegistries.class, immediate = true)
public class SharedMetricRegistries10 extends SharedMetricRegistries {
}
