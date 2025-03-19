/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.impl;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelVisitor;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalkerImpl;

@Component(service = OpenAPIModelWalker.class)
public class OpenAPI31ModelWalkerImpl extends OpenAPIModelWalkerImpl {

    @Override
    public void walk(OpenAPI openAPI, OpenAPIModelVisitor visitor) {
        if (visitor != null && openAPI != null) {
            new Walker31(openAPI, visitor, true).traverseOpenAPI();
        }
    }

    protected static class Walker31 extends Walker {
        public Walker31(OpenAPI openAPI, OpenAPIModelVisitor visitor, boolean previsit) {
            super(openAPI, visitor, previsit);
        }

        @Override
        protected void traverseOpenAPIChildren() {
            super.traverseOpenAPIChildren();

            final Map<String, PathItem> webhooks = openAPI.getWebhooks();
            if (webhooks != null) {
                pathSegments.push("webhooks");
                traversePathItems(webhooks);
                pathSegments.pop();
            }
        }

        protected void traversePathItems(Map<String, PathItem> webhooks) {
            final Map<String, PathItem> updates = map();
            updates.forEach((k, v) -> {
                pathSegments.push(k);
                final PathItem p = traversePathItem(k, v);
                if (p != v) {
                    updates.put(k, p);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(webhooks, updates);
            }
        }

        @Override
        protected void traverseComponentChildren(Components components) {
            super.traverseComponentChildren(components);

            final Map<String, PathItem> pathItems = components.getPathItems();
            if (pathItems != null) {
                pathSegments.push("pathItems");
                traversePathItems(pathItems);
                pathSegments.pop();
            }
        }

        @Override
        protected void traverseSchemaChildren(Schema schema) {
            schema.getAll().forEach((k, v) -> {
                pathSegments.push(k);
                traverseObject(v, k);
                pathSegments.pop();
            });
        }

        protected void traverseObject(Object object, String key) {
            if (object instanceof List<?>) {
                traverseList((List<?>) object);
            } else if (object instanceof Map<?, ?>) {
                traverseMap((Map<?, ?>) object);
            } else {
                traverseModelObject(object, key);
            }
        }

        private void traverseList(List<?> list) {
            int i = 0;
            for (Object element : list) {
                String key = Integer.toString(i);
                pathSegments.push(key);
                traverseObject(element, key);
                pathSegments.pop();
                i++;
            }
        }

        private void traverseMap(Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k instanceof String) {
                    pathSegments.push((String) k);
                    traverseObject(v, (String) k);
                    pathSegments.pop();
                }
            });
        }

        // TODO: Unsure if we should pass through the key here to the walker
        // E.g. validation expects these model objects to only be found at specific locations within the model
        // They can't be validated elsewhere.
        // One option would be to not traverse schemas at all
        // Another would be to optionally do so
        private void traverseModelObject(Object value, String key) {
            if (value instanceof Schema) {
                traverseSchema(null, (Schema) value);
            } else if (value instanceof XML) {
                traverseXML((XML) value);
            } else if (value instanceof ExternalDocumentation) {
                traverseExternalDocs((ExternalDocumentation) value);
            } else if (value instanceof Discriminator) {
                traverseDiscriminator((Discriminator) value);
            } else if (value instanceof Components) {
                traverseComponents((Components) value);
            } else if (value instanceof OpenAPI) {
                traverseOpenAPI((OpenAPI) value);
            } else if (value instanceof Operation) {
                traverseOperation((Operation) value);
            } else if (value instanceof PathItem) {
                traversePathItem(null, (PathItem) value);
            } else if (value instanceof Paths) {
                traversePaths((Paths) value);
            } else if (value instanceof Callback) {
                traverseCallback(null, (Callback) value);
            } else if (value instanceof Header) {
                traverseHeader(null, (Header) value);
            } else if (value instanceof Contact) {
                traverseContact((Contact) value);
            } else if (value instanceof Info) {
                traverseInfo((Info) value);
            } else if (value instanceof License) {
                traverseLicense((License) value);
            } else if (value instanceof Link) {
                traverseLink(null, (Link) value);
            } else if (value instanceof Content) {
                traverseContent((Content) value);
            } else if (value instanceof Encoding) {
                traverseEncoding(null, (Encoding) value);
            } else if (value instanceof Example) {
                traverseExample(null, (Example) value);
            } else if (value instanceof MediaType) {
                traverseMediaType(null, (MediaType) value);
            } else if (value instanceof Parameter) {
                traverseParameter(null, (Parameter) value);
            } else if (value instanceof RequestBody) {
                traverseRequestBody(null, (RequestBody) value);
            } else if (value instanceof APIResponse) {
                traverseResponse(null, (APIResponse) value);
            } else if (value instanceof APIResponses) {
                traverseResponses((APIResponses) value);
            } else if (value instanceof OAuthFlow) {
                traverseOAuthFlow((OAuthFlow) value);
            } else if (value instanceof OAuthFlows) {
                traverseOAuthFlows((OAuthFlows) value);
            } else if (value instanceof SecurityRequirement) {
                traverseSecurityRequirement((SecurityRequirement) value);
            } else if (value instanceof SecurityScheme) {
                traverseSecurityScheme(null, (SecurityScheme) value);
            } else if (value instanceof Server) {
                traverseServer((Server) value);
            } else if (value instanceof ServerVariable) {
                traverseServerVariable(null, (ServerVariable) value);
            } else if (value instanceof Tag) {
                traverseTag((Tag) value);
            }
            // Otherwise it's not a model object, do nothing
        }

    }
}
