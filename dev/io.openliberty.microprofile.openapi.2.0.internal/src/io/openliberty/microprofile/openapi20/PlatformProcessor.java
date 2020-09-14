/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import io.openliberty.microprofile.openapi20.utils.Constants;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

public class PlatformProcessor {

    /**
     * The getComponentsDocument method generates the body of the response for requests to the "/openapi/platform"
     * endpoint, as defibed by section 7.2., "Exposing platform OpenAPIs" of the MP OpenAPI specification.
     * 
     * @param responseFormat
     * @return
     * @throws IOException
     */
    public static String getComponentsDocument(final Format responseFormat) throws IOException {
        
        // Create the variable to return
        final String componentsDocument;

        // Retrieve the list of registered components
        Map<String, String> platforms = getComponents();
        
        // Create the object structure for the components document
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ArrayNode componentList = JsonNodeFactory.instance.arrayNode();
        for (String component : platforms.keySet()) {
            componentList.add(component);
        }
        root.set(Constants.COMPONENTS, componentList);
        
        // Generate the components document in the specified format
        try {
            
            final ObjectMapper mapper;
            if (responseFormat == Format.JSON) {
                mapper = new ObjectMapper();
                componentsDocument = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            } else {
                YAMLFactory factory = new YAMLFactory();
                factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
                factory.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS);
                mapper = new ObjectMapper(factory);
                componentsDocument = mapper.writer().writeValueAsString(root);
            }
        } catch (JsonProcessingException e) {
            throw new IOException(e);
        }

        return componentsDocument;
    }
    
    public static OpenAPI getComponentOpenAPIModel(final String componentName) {
        
        // Create the variable to return
        OpenAPI openAPI = null;

        // Retrieve the list of registered components
        Map<String, String> platforms = getComponents();
        
        // Retrive the OpenAPI YAML document for the specified component
        final String openAPIYAML = platforms.get(componentName);
        if (openAPIYAML != null) {
            // Generate the OpenAPI model from the static file
            InputStream is = new ByteArrayInputStream(openAPIYAML.getBytes());
            OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.YAML);
            openAPI = OpenApiProcessor.modelFromStaticFile(staticFile);
        }

        return openAPI;
    }
    
    /**
     * The getComponents method retrieves the list of components that have registered an
     * mp.openapi.spi.platform.<component> property to indicate that they want to expose an OpenAPI document that
     * describes their APIs.
     * 
     * @return Map<String, String>
     *          The collection of components that expose an OpenAPI document.
     *
     */
    private static Map<String, String> getComponents() {
        /*
         * Manually create the ConfigProvider to ensure that we pick up any components that have dynamically registered
         * to expose an OpenAPI document for their REST APIs. 
         */
        Config config = ConfigProviderResolver.instance()
            .getBuilder()
            .addDefaultSources()
            .addDiscoveredConverters()
            .addDiscoveredSources()
            .build();
        
        /*
         * Filter the MP Config properties to just those that start with "mp.openapi.spi.platform." and then build the
         * collection using the component name as the key and the MP Config property value as the value. 
         */
        Map<String, String> platforms = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
            .filter(name -> name.startsWith(Constants.PLATFORM_SPI_PREFIX))
            .collect(Collectors.toMap(
                name -> name.substring(Constants.PLATFORM_SPI_PREFIX.length()),
                name -> config.getValue(name, String.class)
            ));
        return platforms;
    }
}
