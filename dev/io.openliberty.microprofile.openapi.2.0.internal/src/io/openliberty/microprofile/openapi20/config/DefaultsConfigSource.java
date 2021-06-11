/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.openapi.api.constants.OpenApiConstants;

public class DefaultsConfigSource implements ConfigSource {
    
    private static final Map<String, String> PROPERTIES = Collections.singletonMap(OpenApiConstants.SMALLRYE_PRIVATE_PROPERTIES_ENABLE, "false");
    
    @Override
    public String getName() {
        return "MP OpenAPI defaults";
    }

    @Override
    public int getOrdinal() {
        return 1;
    }

    @Override
    public Map<String, String> getProperties() {
        return PROPERTIES;
    }

    @Override
    public Set<String> getPropertyNames() {
        return PROPERTIES.keySet();
    }

    @Override
    public String getValue(String name) {
        return PROPERTIES.get(name);
    }

}
