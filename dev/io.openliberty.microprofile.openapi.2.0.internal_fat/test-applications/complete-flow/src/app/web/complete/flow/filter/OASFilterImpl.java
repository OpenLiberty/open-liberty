/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package app.web.complete.flow.filter;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

public class OASFilterImpl implements OASFilter {
    @Override
    public Server filterServer(Server server) {
        // Test using app classloader
        // (unusual but this case exposed subtle problems with DefaultsConfigSource)
        ConfigProvider.getConfig(this.getClass().getClassLoader()).getValue("myKey1", String.class);

        // Test using tccl (this is the normal use case)
        ConfigProvider.getConfig().getValue("myKey1", String.class);

        server.setDescription(server.getDescription() + " + from filter");
        return server;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        openAPI.getInfo().setTitle(openAPI.getInfo().getTitle() + " + title from filter");
    }
}
