/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.weld;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.internal.config.AggregatedConfiguration;
import com.ibm.ws.cdi.internal.config.CDIConfiguration;

/**
 * DS for custom CDI 4.0 properties.
 */
@Component(name = "io.openliberty.cdi40.internal.weld.CDI40AggregatedConfigurationImpl", service = { CDIConfiguration.class,
                                                                                                     AggregatedConfiguration.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class CDI40AggregatedConfigurationImpl extends AggregatedConfiguration {

}
