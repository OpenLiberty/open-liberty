/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package product1;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.wab.configure.WABConfiguration;

@Component(configurationPid = "product1",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Product1 implements WABConfiguration {
    // Just a marker service that uses configuration to set the
    // contextName and contextPath service properties
}
