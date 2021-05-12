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
package io.openliberty.microprofile.openapi20.merge;

import static io.openliberty.microprofile.openapi20.utils.OpenAPIUtils.allEqual;
import static io.openliberty.microprofile.openapi20.utils.OpenAPIUtils.notNull;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.MergedOpenAPIProvider;
import io.openliberty.microprofile.openapi20.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.merge.NameProcessor.DocumentNameProcessor;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelVisitor;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker;
import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.api.util.MergeUtil;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * Merges multiple OpenAPI documents together
 * <p>
 * Merging OpenAPI documents is not trivial. In particular the following things have to be considered:
 * <ul>
 * <li>To ensure paths don't overlap, prepend the context root to the front of paths where possible and update any server urls appropriately
 * <li>If paths still overlap, documents cannot be merged
 * <li>Unless it's the same across all documents, move servers and security requirements from the top level to under each path
 * <li>Rename tags, component names and operationIds where necessary to ensure they don't overlap
 * </ul>
 *
 */
public class MergeProcessor {

    private static final TraceComponent tc = Tr.register(MergeProcessor.class);

    private static final Info MERGED_INFO;

    static {
        MERGED_INFO = OASFactory.createInfo();
        MERGED_INFO.setTitle(Constants.MERGED_OPENAPI_DOC_TITLE);
        MERGED_INFO.setVersion(Constants.DEFAULT_OPENAPI_DOC_VERSION);
    }

    /**
     * Create a merged OpenAPI model from a list of OpenAPIProviders.
     * <p>
     * The input objects will not be modified.
     * 
     * @param documents the OpenAPI models to merge
     * @return the merged model
     */
    public static OpenAPIProvider mergeDocuments(List<OpenAPIProvider> documents) {

        MergeProcessor mergeProcessor = new MergeProcessor(documents);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(mergeProcessor, tc, "Beginning merge of OpenAPI documents for " + documents);
        }

        mergeProcessor.process();

        OpenAPIProvider mergedDoc = mergeProcessor.getMergedDocument();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            try {
                Tr.event(mergeProcessor, tc, "Merged document: ", OpenApiSerializer.serialize(mergedDoc.getModel(), Format.JSON));
            } catch (IOException e) {
                Tr.event(mergeProcessor, tc, "Unable to trace merged document", e);
            }
        }

        return mergedDoc;
    }

    /**
     * The providers to merge
     */
    private List<OpenAPIProvider> providers = new ArrayList<>();

    /**
     * Models which have been processed to remove clashes
     */
    private List<OpenAPI> processedModels = new ArrayList<>();

    /**
     * This list of providers used to create the merged document
     */
    private List<OpenAPIProvider> includedProviders = new ArrayList<>();

    /**
     * The path names in use for models processed so far
     */
    private Map<OpenAPIProvider, Set<String>> pathNames = new HashMap<>();

    private NameProcessor nameProcessor = new NameProcessor();

    private List<String> mergeProblems = new ArrayList<>();

    /**
     * Create a new MergeProcessor which will merge the given providers
     * 
     * @param providers the providers to merge
     */
    private MergeProcessor(List<OpenAPIProvider> providers) {
        this.providers = Collections.unmodifiableList(providers);
    }

    private void process() {

        List<InProgressModel> inProgressModels = new ArrayList<>();

        // First loop which just does all the steps necessary to determine whether there are any fatal merge conflicts
        // which result in removing some of the requested merge providers.
        // This ensures that when we come to rename things to avoid non-fatal merge conflicts, we aren't trying to avoid
        // clashing with models that have been removed.
        for (OpenAPIProvider provider : providers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Starting path processing for " + provider);
            }

            // Make a deep copy, to avoid modifying the input model
            OpenAPI model = (OpenAPI) ModelCopy.copy(provider.getModel());

            DocumentNameProcessor documentNameProcessor = nameProcessor.createDocumentNameProcessor();

            // Add context root to the front of paths if possible
            prependPaths(model, provider.getApplicationPath(), documentNameProcessor);

            // Check for clashes
            if (!findAndRecordPathClashes(model, provider)) {
                inProgressModels.add(new InProgressModel(provider, model, documentNameProcessor));

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "No path clashes for " + provider + ", including in merge");
                }
            }
        }

        boolean securityIdentical = isSecurityIdentical(inProgressModels);
        boolean infoIdentical = isInfoIdentical(inProgressModels);
        boolean externalDocsIdentical = isExternalDocsIdentical(inProgressModels);
        boolean serversIdentical = isServersIdentical(inProgressModels);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Security identical = " + securityIdentical);
            Tr.event(this, tc, "Info identical = " + infoIdentical);
            Tr.event(this, tc, "External docs identical = " + externalDocsIdentical);
            Tr.event(this, tc, "Servers identical = " + serversIdentical);
        }

        // Second loop which moves and renames parts of the models where necessary to avoid clashes
        for (InProgressModel inProgress : inProgressModels) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Starting main processing for " + inProgress.provider);
            }

            // Find and process renames
            renameClashingTags(inProgress.model, inProgress.documentNameProcessor);
            renameClashingComponents(inProgress.model, inProgress.documentNameProcessor);
            renameClashingOperationIds(inProgress.model, inProgress.documentNameProcessor);

            // Move security requirements
            if (!securityIdentical) {
                moveSecurityRequirements(inProgress.model);
            }

            if (!infoIdentical) {
                inProgress.model.setInfo(MERGED_INFO);
            }

            if (!externalDocsIdentical) {
                inProgress.model.setExternalDocs(null);
            }

            if (!serversIdentical) {
                moveServersUnderPaths(inProgress.model);
            }

            if (inProgress.documentNameProcessor.hasRenames()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Updating references to renamed elements");
                }

                // Update references
                OpenAPIModelVisitor visitor = new RenameReferenceVisitor(inProgress.documentNameProcessor);
                OpenAPIModelWalker walker = new OpenAPIModelWalker(inProgress.model);
                walker.accept(visitor);
            }

            processedModels.add(inProgress.model);
            includedProviders.add(inProgress.provider);
        }
    }

    private OpenAPIProvider getMergedDocument() {
        // If we've ended up with only one model included, throw away any processing we've done and return the original model
        if (includedProviders.size() == 1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Only one document was able to be merged. Returning that document without changes.");
            }
            return new MergedOpenAPIProvider(includedProviders.get(0).getModel(), mergeProblems);
        }

        OpenAPI merged = OASFactory.createOpenAPI();
        for (OpenAPI model : processedModels) {
            merged = MergeUtil.merge(merged, model);
        }

        merged.setOpenapi(OpenApiConstants.OPEN_API_VERSION);

        return new MergedOpenAPIProvider(merged, mergeProblems);
    }

    /**
     * Finds any clashes between {@code document} and any previously processed document
     * <p>
     * If there are no clashes, the paths from {@code document} are added to {@link #pathNames} and {@code false} is returned.
     * <p>
     * If clashes are found, each clash is added to {@link #mergeProblems} and {@code true} is returned.
     * 
     * @param model the OpenAPI model to search for clashes
     * @param provider the provider that provided the model
     * @return {@code true} if there are any clashes, otherwise {@code false}
     */
    private boolean findAndRecordPathClashes(OpenAPI model, OpenAPIProvider provider) {
        Paths paths = model.getPaths();
        if (paths == null) {
            return false;
        }

        Map<String, PathItem> pathItems = paths.getPathItems();
        if (pathItems == null || pathItems.isEmpty()) {
            return false;
        }

        boolean clashesFound = false;
        for (String path : pathItems.keySet()) {
            for (Entry<OpenAPIProvider, Set<String>> entry : this.pathNames.entrySet()) {
                OpenAPIProvider otherProvider = entry.getKey();
                Set<String> pathNames = entry.getValue();
                if (pathNames.contains(path)) {
                    // Report a clash
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Path " + path + " clashes with " + otherProvider);
                    }
                    mergeProblems.add(Tr.formatMessage(tc, MessageConstants.OPENAPI_MERGE_PROBLEM_PATH_CLASH, path, provider, otherProvider));
                    clashesFound = true;
                }
            }
        }

        if (!clashesFound) {
            this.pathNames.put(provider, pathItems.keySet());
        }

        return clashesFound;
    }

    private void renameClashingTags(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
        for (Tag tag : notNull(document.getTags())) {
            tag.setName(documentNameProcessor.createUniqueName(NameType.TAG, tag.getName(), tag));
        }
    }

    private void renameClashingComponents(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
        Components components = document.getComponents();

        if (components == null) {
            return;
        }

        components.setCallbacks(renameComponents(NameType.CALLBACKS, components.getCallbacks(), documentNameProcessor));
        components.setExamples(renameComponents(NameType.EXAMPLES, components.getExamples(), documentNameProcessor));
        components.setHeaders(renameComponents(NameType.HEADERS, components.getHeaders(), documentNameProcessor));
        components.setLinks(renameComponents(NameType.LINKS, components.getLinks(), documentNameProcessor));
        components.setParameters(renameComponents(NameType.PARAMETERS, components.getParameters(), documentNameProcessor));
        components.setRequestBodies(renameComponents(NameType.REQUEST_BODIES, components.getRequestBodies(), documentNameProcessor));
        components.setResponses(renameComponents(NameType.RESPONSES, components.getResponses(), documentNameProcessor));
        components.setSchemas(renameComponents(NameType.SCHEMAS, components.getSchemas(), documentNameProcessor));
        components.setSecuritySchemes(renameComponents(NameType.SECURITY_SCHEMES, components.getSecuritySchemes(), documentNameProcessor));
    }

    private <T> Map<String, T> renameComponents(NameType nameType, Map<String, T> componentMap, DocumentNameProcessor documentNameProcessor) {
        if (componentMap == null) {
            return null;
        }

        HashMap<String, T> newMap = new HashMap<>();
        for (Entry<String, T> entry : componentMap.entrySet()) {
            String key = entry.getKey();
            newMap.put(documentNameProcessor.createUniqueName(nameType, key, entry.getValue()), entry.getValue());
        }
        return newMap;
    }

    private void renameClashingOperationIds(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
        Paths paths = document.getPaths();
        if (paths == null) {
            return;
        }

        for (PathItem item : notNull(paths.getPathItems()).values()) {
            for (Operation op : notNull(item.getOperations()).values()) {
                op.setOperationId(documentNameProcessor.createUniqueName(NameType.OPERATION_ID, op.getOperationId(), null));
            }
        }
    }

    private void moveSecurityRequirements(OpenAPI document) {
        List<SecurityRequirement> securityRequirements = document.getSecurity();
        if (securityRequirements == null) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Moving security requirements from the top level to under paths");
        }

        Paths paths = document.getPaths();
        if (paths == null) {
            return;
        }

        for (PathItem item : notNull(paths.getPathItems()).values()) {
            for (Operation op : notNull(item.getOperations()).values()) {
                if (op.getSecurity() == null) {
                    op.setSecurity(securityRequirements);
                }
            }
        }

        document.setSecurity(null);
    }

    /**
     * Attempts to remove the context root from any servers and add it to the model paths
     * 
     * @param model the OpenAPI model
     */
    private static void prependPaths(OpenAPI model, String contextRoot, DocumentNameProcessor documentNameProcessor) {
        if (contextRoot == null) {
            return;
        }

        Paths paths = model.getPaths();
        if (paths == null) {
            return;
        }

        List<Server> servers = model.getServers();

        boolean allServersEndWithContextRoot = true;
        // Check whether we can remove the context root from top level servers
        for (Server server : notNull(servers)) {
            if (!serverEndsWithContextRoot(server, contextRoot)) {
                allServersEndWithContextRoot = false;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Server " + server.getUrl() + " does not end with the context root " + contextRoot + ". Context root cannot be prepended to paths.");
                }

                break;
            }
        }

        Map<String, PathItem> pathItems = paths.getPathItems();

        if (allServersEndWithContextRoot) {
            for (Entry<String, PathItem> entry : pathItems.entrySet()) {
                if (!pathServersEndWithContextRoot(entry.getValue(), contextRoot)) {
                    allServersEndWithContextRoot = false;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Server under path " + entry.getKey() + " did not end with context root " + contextRoot + ". Context root cannot be prepended to paths.");
                    }

                }
            }
        }

        if (allServersEndWithContextRoot) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Removing context root " + contextRoot + " from servers and adding it to paths");
            }

            for (Server server : notNull(servers)) {
                removeContextRoot(server, contextRoot);
            }

            Map<String, PathItem> newPathItems = new HashMap<>();
            for (Entry<String, PathItem> entry : pathItems.entrySet()) {
                PathItem pathItem = entry.getValue();

                // Remove context root from servers under path
                for (Server server : notNull(pathItem.getServers())) {
                    removeContextRoot(server, contextRoot);
                }

                // and under the operations under path
                for (Operation op : notNull(pathItem.getOperations()).values()) {
                    for (Server server : notNull(op.getServers())) {
                        removeContextRoot(server, contextRoot);
                    }
                }

                // Then add the context root to the path
                String newPath = addContextRoot(entry.getKey(), contextRoot);
                documentNameProcessor.registerRename(NameType.PATHS, entry.getKey(), newPath);
                newPathItems.put(newPath, pathItem);
            }
            paths.setPathItems(newPathItems);
        }
    }

    /**
     * Returns whether all of the servers related to {@code pathItem} end with the context root
     * <p>
     * This check will consider all servers listed under the path or any of its operations.
     * 
     * @param pathItem the path item to check
     * @param contextRoot the context root to check for
     * @return {@code true} if all servers under {@code pathItem} or any of its operations end with {@code contextRoot}, otherwise {@code false}
     */
    private static boolean pathServersEndWithContextRoot(PathItem pathItem, String contextRoot) {
        boolean allServersEndWithContextRoot = true;

        // Check whether all path servers end with the context root
        for (Server server : notNull(pathItem.getServers())) {
            if (!serverEndsWithContextRoot(server, contextRoot)) {
                allServersEndWithContextRoot = false;
                break;
            }
        }

        if (allServersEndWithContextRoot) {
            // Check whether all operation servers end with the context root
            for (Operation op : notNull(pathItem.getOperations()).values()) {
                for (Server server : notNull(op.getServers())) {
                    if (!serverEndsWithContextRoot(server, contextRoot)) {
                        allServersEndWithContextRoot = false;
                        break;
                    }
                }
            }
        }

        return allServersEndWithContextRoot;
    }

    private static String addContextRoot(String key, String contextRoot) {
        return contextRoot + key;
    }

    private static void removeContextRoot(Server server, String contextRoot) {
        String url = server.getUrl();
        String newUrl;
        if (url.endsWith("/")) {
            newUrl = url.substring(0, url.length() - contextRoot.length() - 1);
        } else {
            newUrl = url.substring(0, url.length() - contextRoot.length());
        }
        server.setUrl(newUrl);
    }

    private static boolean serverEndsWithContextRoot(Server server, String contextRoot) {
        return server.getUrl().endsWith(contextRoot) || server.getUrl().endsWith(contextRoot + "/");
    }

    private static boolean isSecurityIdentical(List<InProgressModel> models) {
        List<List<SecurityRequirement>> reqs = models.stream().map(d -> d.model.getSecurity()).collect(toList());
        return allEqual(reqs, ModelEquality::equals);
    }

    private static boolean isServersIdentical(List<InProgressModel> processedModels) {
        List<List<Server>> servers = processedModels.stream().map(m -> m.model).map(OpenAPI::getServers).collect(toList());
        return allEqual(servers, ModelEquality::equals);
    }

    private static boolean isInfoIdentical(List<InProgressModel> models) {
        List<Info> infos = models.stream().map(m -> m.model).map(OpenAPI::getInfo).collect(toList());
        return allEqual(infos, ModelEquality::equals);
    }

    private static boolean isExternalDocsIdentical(List<InProgressModel> models) {
        List<ExternalDocumentation> docs = models.stream().map(m -> m.model).map(OpenAPI::getExternalDocs).collect(toList());
        return allEqual(docs, ModelEquality::equals);
    }

    private static void moveServersUnderPaths(OpenAPI model) {
        List<Server> servers = model.getServers();
        if (servers == null) {
            return;
        }

        Paths paths = model.getPaths();

        if (paths != null) {
            for (PathItem item : notNull(paths.getPathItems()).values()) {
                if (item.getServers() == null) {
                    item.setServers(servers);
                }
            }
        }

        model.setServers(null);
    }

    private static class InProgressModel {
        private InProgressModel(OpenAPIProvider provider, OpenAPI model, DocumentNameProcessor documentNameProcessor) {
            this.provider = provider;
            this.model = model;
            this.documentNameProcessor = documentNameProcessor;
        }
        private OpenAPIProvider provider;
        private OpenAPI model;
        private DocumentNameProcessor documentNameProcessor;
    }

}
