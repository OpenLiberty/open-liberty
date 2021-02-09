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
package app.web.complete.flow.filter;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

public class OASFilterImpl implements OASFilter {
    @Override
    public Server filterServer(Server server) {
        ConfigProvider.getConfig(this.getClass().getClassLoader()).getValue("myKey1", String.class);
        server.setDescription(server.getDescription() + " + from filter");
        return server;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        openAPI.getInfo().setTitle(openAPI.getInfo().getTitle() + " + title from filter");
    }
}
