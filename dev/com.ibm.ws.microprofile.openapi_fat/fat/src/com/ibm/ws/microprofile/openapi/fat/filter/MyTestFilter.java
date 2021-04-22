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
package com.ibm.ws.microprofile.openapi.fat.filter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;

/**
 * Reads values from config and adds them to the Info block
 */
public class MyTestFilter implements OASFilter {

    @Override
    public void filterOpenAPI(
        OpenAPI openAPI) {

        Info info = OASFactory.createObject(Info.class);

        Config config = ConfigProvider.getConfig();

        // Read from META-INF/microprofile-config.properties
        String description = config.getOptionalValue("filter.description", String.class)
            .orElse("value missing");
        info.setDescription(description);

        // Read from environment variables
        String title = config.getOptionalValue("filter_title", String.class)
            .orElse("value missing");
        info.setTitle(title);

        info.setVersion("1.0");

        openAPI.setInfo(info);
    }

}
