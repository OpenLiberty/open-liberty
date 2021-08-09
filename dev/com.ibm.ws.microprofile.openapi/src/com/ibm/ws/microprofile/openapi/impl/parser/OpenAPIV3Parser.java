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

package com.ibm.ws.microprofile.openapi.impl.parser;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.microprofile.openapi.impl.parser.core.extensions.SwaggerParserExtension;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.AuthorizationValue;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.ParseOptions;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;
import com.ibm.ws.microprofile.openapi.impl.parser.util.ClasspathHelper;
import com.ibm.ws.microprofile.openapi.impl.parser.util.InlineModelResolver;
import com.ibm.ws.microprofile.openapi.impl.parser.util.OpenAPIDeserializer;
import com.ibm.ws.microprofile.openapi.impl.parser.util.RemoteUrl;
import com.ibm.ws.microprofile.openapi.impl.parser.util.ResolverFully;

public class OpenAPIV3Parser implements SwaggerParserExtension {
    private static ObjectMapper JSON_MAPPER, YAML_MAPPER;

    static {
        JSON_MAPPER = ObjectMapperFactory.createJson();
        YAML_MAPPER = ObjectMapperFactory.createYaml();
    }

    @Override
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {

            result = readWithInfo(url, auth);

            if (result.getOpenAPI() != null) {
                String version = result.getOpenAPI().getOpenapi();
                if (auth == null) {
                    auth = new ArrayList<>();
                }
                if (version != null && version.startsWith("3.0")) {
                    if (options != null) {
                        OpenAPIResolver resolver = new OpenAPIResolver(result.getOpenAPI(), auth, null);
                        if (options.isResolve()) {
                            result.setOpenAPI(resolver.resolve());
                        }
                        if (options.isResolveFully()) {
                            result.setOpenAPI(resolver.resolve());
                            new ResolverFully(options.isResolveCombinators()).resolveFully(result.getOpenAPI());
                        } else if (options.isFlatten()) {
                            InlineModelResolver inlineResolver = new InlineModelResolver();
                            inlineResolver.flatten(result.getOpenAPI());
                        }
                    }
                }
            }

        }

        catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));
        }
        return result;
    }

    public OpenAPI read(String location) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return read(location, null, options);
    }

    public OpenAPI read(String location, List<AuthorizationValue> auths, ParseOptions resolve) {
        if (location == null) {
            return null;
        }
        location = location.replaceAll("\\\\", "/");
        OpenAPI output;

        List<SwaggerParserExtension> parserExtensions = getExtensions();
        for (SwaggerParserExtension extension : parserExtensions) {
            output = extension.readLocation(location, auths, resolve).getOpenAPI();
            if (output != null) {
                return output;
            }
        }
        return null;
    }

    public SwaggerParseResult readWithInfo(JsonNode node) {
        OpenAPIDeserializer ser = new OpenAPIDeserializer();
        return ser.deserialize(node, null);
    }

    private ObjectMapper getRightMapper(String data) {
        ObjectMapper mapper;
        if (data.trim().startsWith("{")) {
            mapper = JSON_MAPPER;
        } else {
            mapper = YAML_MAPPER;
        }
        return mapper;
    }

    public SwaggerParseResult readWithInfo(String location, List<AuthorizationValue> auths) {
        String data;

        try {
            location = location.replaceAll("\\\\", "/");
            if (location.toLowerCase().startsWith("http")) {
                data = RemoteUrl.urlToString(location, auths);
            } else {
                final String fileScheme = "file:";
                Path path;
                if (location.toLowerCase().startsWith(fileScheme)) {
                    path = Paths.get(URI.create(location));
                } else {
                    path = Paths.get(location);
                }
                if (Files.exists(path)) {
                    data = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
                } else {
                    data = ClasspathHelper.loadFileFromClasspath(location);
                }
            }

            ObjectMapper mapper = getRightMapper(data);
            JsonNode rootNode = mapper.readTree(data);

            return readWithInfo(rootNode);
        } catch (SSLHandshakeException e) {
            SwaggerParseResult output = new SwaggerParseResult();
            output.setMessages(Arrays.asList("unable to read location `" + location + "` due to a SSL configuration error.  " +
                                             "It is possible that the server SSL certificate is invalid, self-signed, or has an untrusted " +
                                             "Certificate Authority."));
            return output;
        } catch (Exception e) {
            SwaggerParseResult output = new SwaggerParseResult();
            output.setMessages(Arrays.asList("unable to read location `" + location + "`"));
            return output;
        }
    }

    @Override
    public SwaggerParseResult readContents(String swaggerAsString, OpenAPI startingModel, List<AuthorizationValue> auth, ParseOptions options) {
        SwaggerParseResult result = new SwaggerParseResult();
        if (swaggerAsString != null && !"".equals(swaggerAsString.trim())) {
            ObjectMapper mapper = getRightMapper(swaggerAsString);

            if (auth == null) {
                auth = new ArrayList<>();
            }
            if (options != null) {
                if (options.isResolve()) {
                    try {
                        OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
                        JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                        result = deserializer.deserialize(rootNode, startingModel);
                        OpenAPIResolver resolver = new OpenAPIResolver(result.getOpenAPI(), auth, null);
                        result.setOpenAPI(resolver.resolve());

                    } catch (Exception e) {
                        result.setMessages(Arrays.asList(e.getMessage()));
                    }
                } else {
                    try {
                        JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                        result = new OpenAPIDeserializer().deserialize(rootNode, startingModel);

                    } catch (Exception e) {
                        result.setMessages(Arrays.asList(e.getMessage()));
                    }
                }
                if (options.isResolveFully()) {
                    result.setOpenAPI(new OpenAPIResolver(result.getOpenAPI(), auth, null).resolve());
                    new ResolverFully(options.isResolveCombinators()).resolveFully(result.getOpenAPI());

                }
                if (options.isFlatten()) {
                    new InlineModelResolver().flatten(result.getOpenAPI());
                }
            } else {
                try {
                    JsonNode rootNode = mapper.readTree(swaggerAsString.getBytes());
                    result = new OpenAPIDeserializer().deserialize(rootNode, startingModel);

                } catch (Exception e) {
                    result.setMessages(Arrays.asList(e.getMessage()));
                }
            }
        } else {
            result.setMessages(Arrays.asList("No swagger supplied"));
        }
        return result;
    }

    protected List<SwaggerParserExtension> getExtensions() {
        List<SwaggerParserExtension> extensions = new ArrayList<>();

        ServiceLoader<SwaggerParserExtension> loader = ServiceLoader.load(SwaggerParserExtension.class);
        Iterator<SwaggerParserExtension> itr = loader.iterator();
        while (itr.hasNext()) {
            extensions.add(itr.next());
        }
        extensions.add(0, new OpenAPIV3Parser());
        return extensions;
    }

    /**
     * Transform the swagger-model version of AuthorizationValue into a parser-specific one, to avoid
     * dependencies across extensions
     *
     * @param input
     * @return
     */
    protected List<AuthorizationValue> transform(List<AuthorizationValue> input) {
        if (input == null) {
            return null;
        }

        List<AuthorizationValue> output = new ArrayList<>();

        for (AuthorizationValue value : input) {
            AuthorizationValue v = new AuthorizationValue();

            v.setKeyName(value.getKeyName());
            v.setValue(value.getValue());
            v.setType(value.getType());

            output.add(v);
        }

        return output;
    }
}
