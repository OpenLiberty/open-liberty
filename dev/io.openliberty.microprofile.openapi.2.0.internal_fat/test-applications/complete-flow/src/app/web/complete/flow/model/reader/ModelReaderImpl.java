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
package app.web.complete.flow.model.reader;

import java.util.ArrayList;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;

public class ModelReaderImpl implements OASModelReader {
    @Override
    public OpenAPI buildModel() {
        return OASFactory.createObject(OpenAPI.class)
            .info(OASFactory.createObject(Info.class)
                .title("Title from model reader")
                .version("1.0")
                .termsOfService("http://airlinesratingapp.com/terms")
                .contact(OASFactory.createObject(Contact.class)
                    .name("AirlinesRatingApp API Support")
                    .url("http://exampleurl.com/contact")
                    .email("techsupport@airlinesratingapp.com"))
                .license(OASFactory.createObject(License.class)
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
            .security(new ArrayList<SecurityRequirement>())
            .addSecurityRequirement(OASFactory.createObject(SecurityRequirement.class)
                .addScheme("airlinesRatingApp_auth"))
            .servers(new ArrayList<Server>())
            .addServer(OASFactory.createObject(Server.class)
                .url("https://test-server.com:80/#1")
                .description("The test API server #1 - from model reader"))
            .addServer(OASFactory.createObject(Server.class)
                .url("https://test-server.com:80/#2")
                .description("The test API server #2 - from model reader"))
            .externalDocs(OASFactory.createObject(ExternalDocumentation.class)
                .description("instructions for how to deploy this app")
                .url("https://github.com/microservices-api/oas3-airlines/blob/master/README.md"))
            .paths(OASFactory.createObject(Paths.class)
                .addPathItem("/modelReader", OASFactory.createObject(PathItem.class)
                    .GET(OASFactory.createObject(Operation.class)
                        .summary("Retrieve all available airlines")
                        .operationId("getAirlines")
                        .responses(OASFactory.createObject(APIResponses.class)
                            .addAPIResponse("404", OASFactory.createObject(APIResponse.class)
                                .description("No airlines found")
                                .content(OASFactory.createObject(Content.class)
                                    .addMediaType("n/a", OASFactory.createObject(MediaType.class))))))));
    }
}
