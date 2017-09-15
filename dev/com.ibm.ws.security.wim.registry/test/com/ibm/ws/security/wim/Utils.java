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
package com.ibm.ws.security.wim;

import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.wim.ConfigManager;
import com.ibm.ws.security.wim.ProfileManager;
import com.ibm.ws.security.wim.ServiceProvider;

public class Utils {

	public static void doSetConfiguration(ServiceProvider serviceProvider, ServiceReference<ConfigManager> configManagerRef) {
		serviceProvider.setConfiguration(configManagerRef);
	}

	public static void doProfileService(ServiceProvider serviceProvider, ServiceReference<ProfileManager> profileServiceRef) {
		serviceProvider.setProfileservice(profileServiceRef);
    }
}
