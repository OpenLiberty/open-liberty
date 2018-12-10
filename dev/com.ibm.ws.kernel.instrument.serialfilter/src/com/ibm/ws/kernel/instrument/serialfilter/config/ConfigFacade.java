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
package com.ibm.ws.kernel.instrument.serialfilter.config;

import static com.ibm.ws.kernel.instrument.serialfilter.config.ConfigImpl.READ_INTERNAL_CONFIG;
import static com.ibm.ws.kernel.instrument.serialfilter.config.ConfigImpl.READ_SYSTEM_CONFIG;
import static com.ibm.ws.kernel.instrument.serialfilter.config.ConfigImpl.SET_DEFAULT_CONFIG;

public abstract class ConfigFacade {
    private ConfigFacade(){}

    public static Config createConfig() {
        return new ConfigImpl(READ_INTERNAL_CONFIG, READ_SYSTEM_CONFIG);
    }

    public static SimpleConfig getSystemConfigProxy() {
        return ConfigProxy.getConfigProxy();
    }

    public static Config createConfigForUnitTest() {
        return new ConfigImpl(SET_DEFAULT_CONFIG);
    }
}
