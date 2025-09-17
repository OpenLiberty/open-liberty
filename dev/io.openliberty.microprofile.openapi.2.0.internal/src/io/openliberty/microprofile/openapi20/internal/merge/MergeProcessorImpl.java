/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.merge;

import static io.openliberty.microprofile.openapi20.internal.utils.OpenAPIUtils.allEqual;
import static io.openliberty.microprofile.openapi20.internal.utils.OpenAPIUtils.notNull;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.MergedOpenAPIProvider;
import io.openliberty.microprofile.openapi20.internal.merge.NameProcessor.DocumentNameProcessor;
import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelVisitor;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker;
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
public class MergeProcessorImpl implements MergeProcessor {

    private static final TraceComponent tc = Tr.register(MergeProcessorImpl.class);

    private static final Info MERGED_INFO;

    static {
        MERGED_INFO = OASFactory.createInfo();
        MERGED_INFO.setTitle(Constants.MERGED_OPENAPI_DOC_TITLE);
        MERGED_INFO.setVersion(Constants.DEFAULT_OPENAPI_DOC_VERSION);
    }

    @Reference
    protected OpenAPIModelWalker modelWalker;

    /**
     * Create a merged OpenAPI model from a list of OpenAPIProviders.
     * <p>
     * The input objects will not be modified.
     *
     * @param documents the OpenAPI models to merge
     * @return the merged model
     */
    @Override
    public OpenAPIProvider mergeDocuments(List<OpenAPIProvider> documents) {

        MergeProcessorInstance mergeProcessor = createMergeProcessorInstance(documents);

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
     * Create a new MergeProcessorInstance to handle a single merge operation
     *
     * @param documents the documents to be merged
     * @return the new instance
     */
    protected MergeProcessorInstance createMergeProcessorInstance(List<OpenAPIProvider> documents) {
        return new MergeProcessorInstance(documents);
    }

    protected class MergeProcessorInstance {

        /**
         * The providers to merge
         */
        private List<OpenAPIProvider> providers = new ArrayList<>();

        /**
         * Models which have been processed to remove clashes
         */
        private final List<OpenAPI> processedModels = new ArrayList<>();

        /**
         * This list of providers used to create the merged document
         */
        private final List<OpenAPIProvider> includedProviders = new ArrayList<>();

        /**
         * The path names in use for models processed so far
         */
        private final Map<OpenAPIProvider, Set<String>> pathNames = new HashMap<>();

        /**
         * The extensions found on the OpenAPI object in models processed so far
         */
        private final Map<OpenAPIProvider, Map<String, Object>> topLevelExtensions = new HashMap<>();

        private final NameProcessor nameProcessor = new NameProcessor();

        private final List<String> mergeProblems = new ArrayList<>();

        /**
         * Create a new MergeProcessor which will merge the given providers
         *
         * @param providers the providers to merge
         */
        public MergeProcessorInstance(List<OpenAPIProvider> providers) {
            this.providers = Collections.unmodifiableList(providers);
        }

        public void process() {

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
                boolean pathClashes = findAndRecordPathClashes(model, provider);
                boolean extensionClashes = findAndRecordExtensionClashes(model, provider);
                if (!pathClashes && !extensionClashes) {
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
                renameClashingWebhooks(inProgress.model, inProgress.documentNameProcessor);

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
                    modelWalker.walk(inProgress.model, visitor);
                }

                processedModels.add(inProgress.model);
                includedProviders.add(inProgress.provider);
            }
        }

        public OpenAPIProvider getMergedDocument() {
            // If we've ended up with only one model included, throw away any processing we've done and return the original model
            if (includedProviders.size() == 1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Only one document was able to be merged. Returning that document without changes.");
                }
                return new MergedOpenAPIProvider(includedProviders.get(0).getModel(), mergeProblems, includedProviders.get(0).getApplicationPath());
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

        /**
         * Finds any extension clashes between {@code document} and any previously processed document
         * <p>
         * If there are no clashes, the extensions from {@code document} are added to {@link #topLevelExtensions} and {@code false} is returned.
         * <p>
         * If clashes are found, each clash is added to {@link #mergeProblems} and {@code true} is returned.
         *
         * @param model the OpenAPI model to search for clashes
         * @param provider the provider that provided the model
         * @return {@code true} if there are any extension clashes, otherwise {@code false}
         */
        private boolean findAndRecordExtensionClashes(OpenAPI model, OpenAPIProvider provider) {
            Map<String, Object> extensions = model.getExtensions();
            if (extensions == null || extensions.isEmpty()) {
                // Can't clash if we have no extensions
                return false;
            }

            boolean clashesFound = false;
            for (Entry<OpenAPIProvider, Map<String, Object>> entry : this.topLevelExtensions.entrySet()) {
                OpenAPIProvider otherProvider = entry.getKey();
                Map<String, Object> otherExtensions = entry.getValue();
                for (Entry<String, Object> extensionEntry : extensions.entrySet()) {
                    String key = extensionEntry.getKey();
                    Object value = extensionEntry.getValue();
                    Object otherValue = otherExtensions.get(key);
                    if (otherValue != null && !Objects.equals(value, otherValue)) {
                        mergeProblems.add(Tr.formatMessage(tc, MessageConstants.OPENAPI_MERGE_PROBLEM_EXTENSION_CLASH, key, provider, otherProvider));
                        clashesFound = true;
                    }
                }
            }

            if (!clashesFound) {
                this.topLevelExtensions.put(provider, extensions);
            }

            return clashesFound;
        }

        private void renameClashingTags(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
            for (Tag tag : notNull(document.getTags())) {
                tag.setName(documentNameProcessor.createUniqueName(NameType.TAG, tag.getName(), tag));
            }
        }

        protected void renameClashingComponents(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
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

        protected <T> Map<String, T> renameComponents(NameType nameType, Map<String, T> componentMap, DocumentNameProcessor documentNameProcessor) {
            if (componentMap == null) {
                return null;
            }

            Map<String, T> newMap = new LinkedHashMap<>(); //We must preserve the order of components as the order is human visible.
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

        protected void renameClashingWebhooks(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
            // No-op for OpenAPI 3.0
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

        if (contextRoot.endsWith("/")) {
            contextRoot = contextRoot.substring(0, contextRoot.length() - 1);
        }

        if (contextRoot.isEmpty()) {
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

            Map<String, PathItem> newPathItems = new LinkedHashMap<>(); //We must preserve the order of path items as the order is human visible.
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

            // In some cases, removing the context root can leave all servers with no information at all.
            // In these cases they can be removed.
            boolean allServersEmpty = notNull(servers).stream()
                                                      .allMatch(MergeProcessorImpl::isServerEmpty);

            if (allServersEmpty) {
                model.setServers(null);
            }
        }
    }

    /**
     * Check whether a server element contains no useful data
     *
     * @param server the server element to check
     * @return true if the server element has no extensions, no description, no variables and a URL which is empty or {@code "/"}
     */
    private static boolean isServerEmpty(Server server) {
        Map<String, Object> extensions = server.getExtensions();
        if (extensions != null && !extensions.isEmpty()) {
            return false;
        }

        String description = server.getDescription();
        if (description != null && !description.isEmpty()) {
            return false;
        }

        Map<String, ?> variables = server.getVariables();
        if (variables != null && !variables.isEmpty()) {
            return false;
        }

        String url = server.getUrl();
        if (url != null && !url.isEmpty() && !url.equals("/")) {
            return false;
        }

        return true;
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

        private final OpenAPIProvider provider;
        private final OpenAPI model;
        private final DocumentNameProcessor documentNameProcessor;
    }

}
