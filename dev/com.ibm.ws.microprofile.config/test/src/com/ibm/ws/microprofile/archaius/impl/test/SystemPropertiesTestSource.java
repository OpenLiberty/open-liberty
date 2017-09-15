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
package com.ibm.ws.microprofile.archaius.impl.test;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

public class SystemPropertiesTestSource extends PropertiesTestSource implements ConfigSource {

    private static final int DEFAULT_SYSTEM_ORDINAL = ConfigConstants.ORDINAL_SYSTEM_PROPERTIES;

    public SystemPropertiesTestSource() {
        super(System.getProperties());
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return DEFAULT_SYSTEM_ORDINAL;
    }

}
