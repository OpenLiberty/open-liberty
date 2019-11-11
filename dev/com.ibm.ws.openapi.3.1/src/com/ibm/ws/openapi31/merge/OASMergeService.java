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
package com.ibm.ws.openapi31.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;
import com.ibm.ws.openapi31.OASProviderWrapper;

public class OASMergeService {

    public static final String OA_TAGS = "$.tags";
    public static final String OA_COMPONENTS = "$.components";
    public static final String OA_COMPONENTS_EXAMPLES = "$.components.examples";
    public static final String OA_COMPONENTS_CALLBACKS = "$.components.callbacks";
    public static final String OA_COMPONENTS_LINKS = "$.components.links";
    public static final String OA_COMPONENTS_HEADERS = "$.components.headers";
    public static final String OA_COMPONENTS_PARAMETERS = "$.components.parameters";
    public static final String OA_COMPONENTS_REQUEST_BODIES = "$.components.requestBodies";
    public static final String OA_COMPONENTS_RESPONSES = "$.components.responses";
    public static final String OA_COMPONENTS_SECURITY_SHEMES = "$.components.securitySchemes";
    public static final String OA_COMPONENTS_SCHEMAS = "$.components.schemas";
    public static final String OA_PATHS = "$.paths";
    public static final String OA_OPERATION_ID = "$.paths..operationId";

    private static final TraceComponent tc = Tr.register(OASMergeService.class,com.ibm.ws.openapi31.TraceConstants.TRACE_GROUP, com.ibm.ws.openapi31.TraceConstants.TRACE_BUNDLE_CORE);
    //represents how many providers referencing specific OA object
    private final Map<String, Integer> referencesCount = new HashMap<>();
    private final Set<String> operationIds = new HashSet<>();
    private final List<OASProviderWrapper> OASProviders = new LinkedList<>();

    private final Map<String, Map<String, String>> conflictsMap = new HashMap<>();
    private volatile OASProviderWrapper incomingProvider = null;
    private OpenAPI OAInProgress = null;

    public OASMergeService() {}

    public synchronized void addAPIProvider(OASProviderWrapper inDoc) {

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Started merging OASProvider: " + inDoc.getOpenAPIProvider());
        }
        conflictsMap.clear();

        inDoc.getJsonPathKeys().clear();

        OASProviders.add(inDoc);
        incomingProvider = inDoc;
        OpenAPI openAPI = inDoc.getOpenAPI();

        OAInProgress = openAPI;
        addServers();
        addTags();
        addPaths();
        addComponents();
        addSecurity();
        addExtensions();

        List<Operation> operations = getAllOperations(OAInProgress);
        for (Operation operation : operations) {
            String operationId = operation.getOperationId();
            if (operationId == null)
                continue;
            if (operationIds.contains(operationId)) {
                int count = 0;
                String newOperationId = operationId + "_" + count;
                while (operationIds.contains(newOperationId)) {
                    count++;
                    newOperationId = operationId + "_" + count;
                }
                getConflictsMap(OA_OPERATION_ID).put(operationId, newOperationId);
                operation.setOperationId(newOperationId);
                operationIds.add(newOperationId);
            } else {
                operationIds.add(operationId);
            }
        }

        renameReferences();

        incomingProvider = null;
        OAInProgress = null;

        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Done merging OASProvider: " + inDoc.getOpenAPIProvider());
        }
    }

    public synchronized void removeAPIProvider(OASProviderWrapper inDoc) {
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Started removing OASProvider: " + inDoc.getOpenAPIProvider());
        }
        decrementReferencesCount(inDoc);

        Set<String> opIds = getAllOperations(inDoc.getOpenAPI()).stream().map(op -> op.getOperationId()).collect(Collectors.toSet());

        operationIds.removeAll(opIds);

        OASProviders.remove(inDoc);
        inDoc.getJsonPathKeys().clear();
        if (OpenAPIUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Done removing OASProvider: " + inDoc.getOpenAPIProvider());
        }
    }

    private void incrementReferencesCount(String jpKey) {
        Integer c = referencesCount.get(jpKey);
        int count = c != null ? c.intValue() : 0;
        count++;
        referencesCount.put(jpKey, count);
    }

    private void decrementReferencesCount(OASProviderWrapper apiProvider) {
        if (OpenAPIUtils.isDumpEnabled(tc)) {
            Tr.dump(tc, "References before removal: " + debugPrintReferences());
        }
        for (String jpKey : apiProvider.getJsonPathKeys()) {
            Integer c = referencesCount.get(jpKey);
            int count = c != null ? c.intValue() : 0;
            count--;
            if (count > 0)
                referencesCount.put(jpKey, count);
            else {
                referencesCount.remove(jpKey);
            }
        }
        if (OpenAPIUtils.isDumpEnabled(tc)) {
            Tr.dump(tc, "References after removal: " + debugPrintReferences());
        }
    }

    private void addPaths() {
        Paths incomingPaths = OAInProgress.getPaths();
        if (incomingPaths == null || incomingPaths.isEmpty())
            return;
        incomingPaths.forEach((path, pathItem) -> {
            addKey(OA_PATHS + "." + path);
        });
    }

    private void addTags() {

        List<Tag> incomingTags = OAInProgress.getTags();

        if (incomingTags == null || incomingTags.isEmpty())
            return;

        incomingTags.stream().forEach(tag -> {

            String tagName = tag.getName();
            String jpKey = OA_TAGS + "[?(@.name==\'" + tagName + "\')]";
            int count = 1;
            while (detectConflict(jpKey, tag)) {
                tag.setName(tagName + count);
                jpKey = OA_TAGS + "[?(@.name==\'" + tag.getName() + "\')]";
                count++;
            }
            if (!tagName.equals(tag.getName()))
                setConlictPair(OA_TAGS, tagName, tag.getName());
            addKey(jpKey);
        });
        return;
    }

    private boolean shouldAddBasePath(List<Server> servers) {
        if (servers.size() != 1)
            return false;
        Server server = servers.get(0);
        if (server == null)
            return false;
        String url = server.getUrl();
        if (url == null)
            return false;
        if (url.equals("/"))
            return false;
        if (!url.startsWith("/"))
            return false;
        if (url.contains("{") || url.contains("}"))
            return false;
        if (OpenAPIUtils.isDebugEnabled(tc)) {
            Tr.debug(tc, "Found 1 server with a basepath. Adding to paths");
        }
        return true;
    }

    private void addServers() {

        List<Server> incomingServers = OAInProgress.getServers();
        if (incomingServers == null || incomingServers.isEmpty())
            return;

        Paths incomingPaths = OAInProgress.getPaths();
        if (incomingPaths == null || incomingPaths.isEmpty())
            return;

        if (shouldAddBasePath(incomingServers)) {
            Paths paths = OASFactory.createObject(Paths.class);
            paths.setExtensions(incomingPaths.getExtensions());
            incomingPaths.forEach((path, pathItem) -> {
                if (pathItem.getServers() == null) {
                    String serverUrl = incomingServers.get(0).getUrl();
                    String url = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length()) : serverUrl;
                    paths.addPathItem(url + path, pathItem);
                } else {
                    paths.addPathItem(path, pathItem);
                }
            });

            OAInProgress.setPaths(paths);

        } else {
            incomingPaths.values().forEach(path -> {
                if (path.getServers() == null)
                    path.setServers(incomingServers);
            });
        }
        OAInProgress.setServers(null);
    }

    //TODO: merge extensions of the same type
    private void addExtensions() {
        Map<String, Object> incomingExtensions = OAInProgress.getExtensions();
        if (incomingExtensions == null || incomingExtensions.isEmpty())
            return;

        incomingExtensions.forEach((k, v) -> {
            addKey("$." + k);
        });
    }

    private <T> Map<String, T> handleMapRename(Map<String, T> map, String jpKeyPrefix) {
        if (map == null || map.isEmpty()) {
            return map;
        }
        Map<String, T> new_map = new HashMap<>();

        map.forEach((k, v) -> {
            String jpathKey = jpKeyPrefix + "." + k;
            int count = 1;
            String new_key = k;
            while (detectConflict(jpathKey, v)) {
                new_key = k + count;
                jpathKey = jpKeyPrefix + "." + new_key;
                count++;
            }
            if (!k.equals(new_key)) {
                setConlictPair(jpKeyPrefix, k, new_key);
            }
            new_map.put(new_key, v);
            addKey(jpathKey);
        });
        return new_map;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void addComponents() {

        Components incomingComponents = OAInProgress.getComponents();

        if (incomingComponents == null)
            return;
        Map m = null;
        m = handleMapRename(incomingComponents.getExamples(), OA_COMPONENTS_EXAMPLES);
        incomingComponents.setExamples(m);

        m = handleMapRename(incomingComponents.getCallbacks(), OA_COMPONENTS_CALLBACKS);
        incomingComponents.setCallbacks(m);

        m = handleMapRename(incomingComponents.getHeaders(), OA_COMPONENTS_HEADERS);
        incomingComponents.setHeaders(m);

        m = handleMapRename(incomingComponents.getLinks(), OA_COMPONENTS_LINKS);
        incomingComponents.setLinks(m);

        m = handleMapRename(incomingComponents.getParameters(), OA_COMPONENTS_PARAMETERS);
        incomingComponents.setParameters(m);

        m = handleMapRename(incomingComponents.getRequestBodies(), OA_COMPONENTS_REQUEST_BODIES);
        incomingComponents.setRequestBodies(m);

        m = handleMapRename(incomingComponents.getResponses(), OA_COMPONENTS_RESPONSES);
        incomingComponents.setResponses(m);

        m = handleMapRename(incomingComponents.getSecuritySchemes(), OA_COMPONENTS_SECURITY_SHEMES);
        incomingComponents.setSecuritySchemes(m);

        m = handleMapRename(incomingComponents.getSchemas(), OA_COMPONENTS_SCHEMAS);
        incomingComponents.setSchemas(m);

        //special case do not rename vendor extensions
        Map<String, Object> incomingExt = incomingComponents.getExtensions();
        if (incomingExt != null && !incomingExt.isEmpty()) {
            incomingExt.forEach((k, v) -> {
                addKey(OA_COMPONENTS + "." + k);
            });
        }
    }

    private void addSecurity() {

        List<SecurityRequirement> incomingSecReq = OAInProgress.getSecurity();
        if (incomingSecReq == null || incomingSecReq.isEmpty())
            return;

        Paths incomingPaths = OAInProgress.getPaths();

        if (incomingPaths == null)
            return;

        List<Operation> operations = incomingPaths.values().stream().flatMap(path -> {
            return Stream.of(path.getGET(), path.getPOST(), path.getPUT(),
                             path.getDELETE(), path.getHEAD(),
                             path.getOPTIONS(), path.getPATCH(), path.getTRACE());

        }).filter(operation -> operation != null && operation.getSecurity() == null).collect(Collectors.toList());

        operations.forEach(operation -> {
            operation.setSecurity(incomingSecReq);
        });

    }

    public String debugPrintReferences() {
        StringBuilder sb = new StringBuilder();
        sb.append("Number of APIProvider references per key:\n");
        referencesCount.forEach((k, v) -> sb.append(k + ":" + v + "\n"));
        return sb.toString();
    }

    private boolean detectConflict(String jpKey, Object newItem) {
        if (referencesCount.get(jpKey) == null) {
            return false;
        } else {
            OASProviderWrapper wrapper = OASProviders.stream().filter(provider -> provider.getJsonPathKeys().contains(jpKey)).findFirst().orElse(null);
            if (wrapper == null) {
                return false;
            }
            Object item = getItemAtKey(jpKey, wrapper.getOpenAPI());
            return !item.equals(newItem);
        }
    }

    private void addKey(String jpKey) {
        incomingProvider.getJsonPathKeys().add(jpKey);
        incrementReferencesCount(jpKey);
    }

    private void setConlictPair(String type, String original, String newName) {
        Map<String, String> conflictPairs = getConflictsMap(type);
        conflictPairs.put(original, newName);
    }

    private Map<String, String> getConflictsMap(String type) {
        Map<String, String> conflicts = conflictsMap.get(type);
        if (conflicts == null) {
            conflicts = new HashMap<>();
            conflictsMap.put(type, conflicts);
        }
        return conflicts;
    }

    private Object getItemAtKey(String jpKey, OpenAPI openAPI) {

        if (OpenAPIUtils.isDebugEnabled(tc)) {
            Tr.debug(tc, "Trying to get item at key: " + jpKey);
        }

        if (jpKey.startsWith(OA_TAGS)) {

            if (openAPI.getTags() == null)
                return null;
            Pattern p = Pattern.compile("name=='(.*)'");
            Matcher m = p.matcher(jpKey);
            if (m.find()) {
                String tagName = m.group(1);
                return openAPI.getTags().stream().filter(tag -> tag.getName().equals(tagName)).findFirst().orElse(null);
            }

        } else if (jpKey.startsWith(OA_PATHS)) {
            String pathKey = jpKey.split("\\.")[2];
            return openAPI.getPaths().get(pathKey);
        } else if (jpKey.startsWith(OA_COMPONENTS)) {

            String items[] = jpKey.split("\\.");
            String itemKey = items.length >= 4 ? items[3] : items[2];

            Components components = openAPI.getComponents();

            if (components == null)
                return null;

            if (jpKey.startsWith(OA_COMPONENTS_EXAMPLES)) {
                return components.getExamples().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_CALLBACKS)) {
                return components.getCallbacks().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_HEADERS)) {
                return components.getHeaders().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_LINKS)) {
                return components.getLinks().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_PARAMETERS)) {
                return components.getParameters().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_REQUEST_BODIES)) {
                return components.getRequestBodies().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_RESPONSES)) {
                return components.getResponses().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_SECURITY_SHEMES)) {
                return components.getSecuritySchemes().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS_SCHEMAS)) {
                return components.getSchemas().get(itemKey);
            } else if (jpKey.startsWith(OA_COMPONENTS + ".x-")) {
                return components.getExtensions().get(itemKey);
            }

        } else if (jpKey.startsWith("$.x-")) {
            String extensionKey = jpKey.substring("$.".length());
            return openAPI.getExtensions().get(extensionKey);
        }
        return null;
    }

    private void renameReferences() {

        OpenAPI openAPI = OAInProgress;

        OASRenameVisitor renameVisitor = new OASRenameVisitor(openAPI, conflictsMap);
        renameVisitor.renameRefs();
    }

    private <T> Map<String, T> mergeMaps(Map<String, T> resultMap, Map<String, T> inMap) {
        if (inMap == null || inMap.isEmpty()) {
            return resultMap;
        }
        if (resultMap == null)
            resultMap = new HashMap<>();
        resultMap.putAll(inMap);
        return resultMap;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mergeOpenAPI(OpenAPI result, OpenAPI in) {

        List<Tag> inTags = in.getTags();
        if (inTags != null && !inTags.isEmpty()) {

            Set<String> tagNames = null;
            if (result.getTags() != null)
                tagNames = result.getTags().stream().map(tag -> tag.getName()).collect(Collectors.toSet());
            for (Tag tag : inTags) {
                if (tagNames == null || !tagNames.contains(tag.getName())) {
                    result.addTag(tag);
                }
            }
        }
        Components inComponents = in.getComponents();
        if (inComponents != null) {
            if (result.getComponents() == null)
                result.setComponents(OASFactory.createObject(Components.class));
            Components resComponents = result.getComponents();

            Map resMap = mergeMaps(resComponents.getCallbacks(), inComponents.getCallbacks());
            resComponents.setCallbacks(resMap);
            resMap = mergeMaps(resComponents.getExamples(), inComponents.getExamples());
            resComponents.setExamples(resMap);
            resMap = mergeMaps(resComponents.getExtensions(), inComponents.getExtensions());
            resComponents.setExtensions(resMap);
            resMap = mergeMaps(resComponents.getHeaders(), inComponents.getHeaders());
            resComponents.setHeaders(resMap);
            resMap = mergeMaps(resComponents.getLinks(), inComponents.getLinks());
            resComponents.setLinks(resMap);
            resMap = mergeMaps(resComponents.getParameters(), inComponents.getParameters());
            resComponents.setParameters(resMap);
            resMap = mergeMaps(resComponents.getRequestBodies(), inComponents.getRequestBodies());
            resComponents.setRequestBodies(resMap);
            resMap = mergeMaps(resComponents.getResponses(), inComponents.getResponses());
            resComponents.setResponses(resMap);
            resMap = mergeMaps(resComponents.getSchemas(), inComponents.getSchemas());
            resComponents.setSchemas(resMap);
            resMap = mergeMaps(resComponents.getSecuritySchemes(), inComponents.getSecuritySchemes());
            resComponents.setSecuritySchemes(resMap);
        }

        Paths inPaths = in.getPaths();
        if (inPaths != null && !inPaths.isEmpty()) {
            if (result.getPaths() == null)
                result.setPaths(OASFactory.createObject(Paths.class));
            Paths resultPaths = result.getPaths();
            inPaths.forEach((pathKey, pathItem) -> {
                resultPaths.addPathItem(pathKey, pathItem);
            });
        }

        //TODO: merge extensions if they are same type
        Map<String, Object> extensions = in.getExtensions();
        if (extensions != null && !extensions.isEmpty()) {
            extensions.forEach((extKey, extVal) -> {
                result.addExtension(extKey, extVal);
            });
        }
    }

    static public List<Operation> getAllOperations(OpenAPI openAPI) {
        List<Operation> operations = new ArrayList<>();
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                operations.addAll(getAllOperationsForPath(pathItem));
            });
        }
        return operations;
    }

    static public List<Operation> getAllOperationsForPath(PathItem pathItem) {
        Stream<Operation> ops = Arrays.asList(pathItem.getGET(), pathItem.getPOST(), pathItem.getPUT(),
                                              pathItem.getDELETE(), pathItem.getHEAD(),
                                              pathItem.getOPTIONS(), pathItem.getPATCH(),
                                              pathItem.getTRACE()).stream();
        ops = ops.filter(Objects::nonNull);
        return ops.collect(Collectors.toList());
    }

}
