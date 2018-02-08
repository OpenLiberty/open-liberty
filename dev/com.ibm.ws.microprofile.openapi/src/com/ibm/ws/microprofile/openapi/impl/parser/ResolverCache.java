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

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.AuthorizationValue;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefType;
import com.ibm.ws.microprofile.openapi.impl.parser.util.DeserializationUtils;
import com.ibm.ws.microprofile.openapi.impl.parser.util.OpenAPIDeserializer;
import com.ibm.ws.microprofile.openapi.impl.parser.util.PathUtils;
import com.ibm.ws.microprofile.openapi.impl.parser.util.RefUtils;

/**
 * A class that caches values that have been loaded so we don't have to repeat
 * expensive operations like:
 * 1) reading a remote URL with authorization (e.g. using RemoteURL.java)
 * 2) reading the contents of a file into memory
 * 3) extracting a sub object from a json/yaml tree
 * 4) de-serializing json strings into objects
 */
public class ResolverCache {

    private static final Pattern SCHEMAS_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "schemas/(?<name>.+)");
    private static final Pattern RESPONSES_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "responses/(?<name>.+)");
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "parameters/(?<name>.+)");
    private static final Pattern REQUEST_BODIES_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "requestBodies/(?<name>.+)");
    private static final Pattern EXAMPLES_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "examples/(?<name>.+)");
    private static final Pattern LINKS_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "links/(?<name>.+)");
    private static final Pattern CALLBACKS_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "callbacks/(?<name>.+)");
    private static final Pattern HEADERS_PATTERN = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "headers/(?<name>.+)");
    private static final Pattern SECURITY_SCHEMES = Pattern.compile("^" + RefType.COMPONENTS.getInternalPrefix() + "securitySchemes/(?<name>.+)");
    private static final Pattern PATHS_PATTERN = Pattern.compile("^" + RefType.PATH.getInternalPrefix() + "(?<name>.+)");

    private final OpenAPI openApi;
    private final List<AuthorizationValue> auths;
    private final Path parentDirectory;
    private final String rootPath;
    private final Map<String, Object> resolutionCache = new HashMap<>();
    private final Map<String, String> externalFileCache = new HashMap<>();
    private final Set<String> referencedModelKeys = new HashSet<>();

    /*
     * a map that stores original external references, and their associated renamed references
     */
    private final Map<String, String> renameCache = new HashMap<>();

    public ResolverCache(OpenAPI openApi, List<AuthorizationValue> auths, String parentFileLocation) {
        this.openApi = openApi;
        this.auths = auths;
        this.rootPath = parentFileLocation;

        if (parentFileLocation != null) {
            if (parentFileLocation.startsWith("http")) {
                parentDirectory = null;
            } else {
                parentDirectory = PathUtils.getParentDirectoryOfFile(parentFileLocation);
            }
        } else {
            File file = new File(".");
            parentDirectory = file.toPath();
        }

    }

    public <T> T loadRef(String ref, RefFormat refFormat, Class<T> expectedType) {
        if (refFormat == RefFormat.INTERNAL) {
            //we don't need to go get anything for internal refs
            Object loadedRef = loadInternalRef(ref);

            try {
                return expectedType.cast(loadedRef);
            } catch (Exception e) {
                return null;
            }
        }

        final String[] refParts = ref.split("#/");

        if (refParts.length > 2) {
            throw new RuntimeException("Invalid ref format: " + ref);
        }

        final String file = refParts[0];
        final String definitionPath = refParts.length == 2 ? refParts[1] : null;

        //we might have already resolved this ref, so check the resolutionCache
        Object previouslyResolvedEntity = resolutionCache.get(ref);

        if (previouslyResolvedEntity != null) {
            return expectedType.cast(previouslyResolvedEntity);
        }

        //we have not resolved this particular ref
        //but we may have already loaded the file or url in question
        String contents = externalFileCache.get(file);

        if (contents == null) {
            if (parentDirectory != null) {
                contents = RefUtils.readExternalRef(file, refFormat, auths, parentDirectory);
            } else if (rootPath != null) {
                contents = RefUtils.readExternalUrlRef(file, refFormat, auths, rootPath);
            }
            externalFileCache.put(file, contents);
        }

        if (definitionPath == null) {
            T result = DeserializationUtils.deserialize(contents, file, expectedType);
            resolutionCache.put(ref, result);
            return result;
        }

        //a definition path is defined, meaning we need to "dig down" through the JSON tree and get the desired entity
        JsonNode tree = DeserializationUtils.deserializeIntoTree(contents, file);

        String[] jsonPathElements = definitionPath.split("/");
        for (String jsonPathElement : jsonPathElements) {
            tree = tree.get(unescapePointer(jsonPathElement));
            //if at any point we do find an element we expect, print and error and abort
            if (tree == null) {
                throw new RuntimeException("Could not find " + definitionPath + " in contents of " + file);
            }
        }

        T result;
        if (expectedType.equals(Schema.class)) {
            OpenAPIDeserializer deserializer = new OpenAPIDeserializer();
            result = (T) deserializer.getSchema((ObjectNode) tree, definitionPath.replace("/", "."), null);
        } else {
            result = DeserializationUtils.deserialize(tree, file, expectedType);
        }
        resolutionCache.put(ref, result);

        return result;
    }

    private Object loadInternalRef(String ref) {
        Object result = null;

        if (ref.startsWith("#/paths")) {
            result = getFromMap(ref, openApi.getPaths(), PATHS_PATTERN);
        } else if (openApi.getComponents() != null) {
            if (ref.startsWith("#/components/schemas")) {
                result = getFromMap(ref, openApi.getComponents().getSchemas(), SCHEMAS_PATTERN);
            } else if (ref.startsWith("#/components/requestBodies")) {
                result = getFromMap(ref, openApi.getComponents().getRequestBodies(), REQUEST_BODIES_PATTERN);
            } else if (ref.startsWith("#/components/examples")) {
                result = getFromMap(ref, openApi.getComponents().getExamples(), EXAMPLES_PATTERN);
            } else if (ref.startsWith("#/components/responses")) {
                result = getFromMap(ref, openApi.getComponents().getResponses(), RESPONSES_PATTERN);
            } else if (ref.startsWith("#/components/parameters")) {
                result = getFromMap(ref, openApi.getComponents().getParameters(), PARAMETERS_PATTERN);
            } else if (ref.startsWith("#/components/links")) {
                result = getFromMap(ref, openApi.getComponents().getLinks(), LINKS_PATTERN);
            } else if (ref.startsWith("#/components/headers")) {
                result = getFromMap(ref, openApi.getComponents().getHeaders(), HEADERS_PATTERN);
            } else if (ref.startsWith("#/components/callbacks")) {
                result = getFromMap(ref, openApi.getComponents().getCallbacks(), CALLBACKS_PATTERN);
            } else if (ref.startsWith("#/components/securitySchemes")) {
                result = getFromMap(ref, openApi.getComponents().getSecuritySchemes(), SECURITY_SCHEMES);
            }
        }

        return result;

    }

    private String unescapePointer(String jsonPathElement) {
        // Unescape the JSON Pointer segment using the algorithm described in RFC 6901, section 4:
        // https://tools.ietf.org/html/rfc6901#section-4
        // First transform any occurrence of the sequence '~1' to '/'
        jsonPathElement = jsonPathElement.replaceAll("~1", "/");
        // Then transforming any occurrence of the sequence '~0' to '~'.
        return jsonPathElement.replaceAll("~0", "~");
    }

    private Object getFromMap(String ref, Map map, Pattern pattern) {
        final Matcher parameterMatcher = pattern.matcher(ref);

        if (parameterMatcher.matches()) {
            final String paramName = unescapePointer(parameterMatcher.group("name"));

            if (map != null) {
                return map.get(paramName);
            }
        }
        return null;
    }

    public boolean hasReferencedKey(String modelKey) {
        if (referencedModelKeys == null) {
            return false;
        }
        return referencedModelKeys.contains(modelKey);
    }

    public void addReferencedKey(String modelKey) {
        referencedModelKeys.add(modelKey);
    }

    public String getRenamedRef(String originalRef) {
        return renameCache.get(originalRef);
    }

    public void putRenamedRef(String originalRef, String newRef) {
        renameCache.put(originalRef, newRef);
    }

    public Map<String, Object> getResolutionCache() {
        return Collections.unmodifiableMap(resolutionCache);
    }

    public Map<String, String> getExternalFileCache() {
        return Collections.unmodifiableMap(externalFileCache);
    }

    public Map<String, String> getRenameCache() {
        return Collections.unmodifiableMap(renameCache);
    }
}
