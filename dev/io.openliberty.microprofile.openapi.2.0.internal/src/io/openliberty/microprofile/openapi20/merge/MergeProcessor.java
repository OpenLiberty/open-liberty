package io.openliberty.microprofile.openapi20.merge;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.microprofile.openapi20.MergedOpenAPIProvider;
import io.openliberty.microprofile.openapi20.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelVisitor;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker;
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

    /**
     * Create a merged OpenAPI model from a list of OpenAPIProviders.
     * <p>
     * The input objects will not be modified.
     * 
     * @param documents the OpenAPI models to merge
     * @return the merged model
     */
    public static OpenAPIProvider mergeDocuments(List<OpenAPIProvider> documents) {

        MergeProcessor mergeProcessor = new MergeProcessor();
        for (OpenAPIProvider document : documents) {
            mergeProcessor.add(document);
        }

        mergeProcessor.process();

        OpenAPIProvider mergedDoc = mergeProcessor.getMergedDocument();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            try {
                Tr.event(tc, "Merged document: ", OpenApiSerializer.serialize(mergedDoc.getModel(), Format.JSON));
            } catch (IOException e) {
                Tr.event(tc, "Unable to trace merged document", e);
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
    private Set<String> pathNames = new HashSet<>();

    /**
     * The unique names and their values which are in use for models processed so far, grouped by the type of name
     */
    private Map<NameType, Map<String, Object>> namesInUse = new HashMap<>();

    private List<String> mergeProblems = new ArrayList<>();

    private void add(OpenAPIProvider document) {
        providers.add(document);
    }

    private void process() {

        boolean securityIdentical = isSecurityIdentical(providers);

        for (OpenAPIProvider provider : providers) {
            // Make a deep copy, to avoid modifying the input model
            OpenAPI model = (OpenAPI) ModelCopy.copy(provider.getModel());

            RenameHolder renameHolder = new RenameHolder();

            // Add context root to the front of paths if possible
            prependPaths(model, provider.getApplicationPath(), renameHolder);

            // Check for clashes
            if (findPathClashes(model, provider)) {
                break;
            }

            // Find and process renames
            renameClashingTags(model, renameHolder);
            renameClashingComponents(model, renameHolder);
            renameClashingOperationIds(model, renameHolder);

            // Move security requirements
            if (!securityIdentical) {
                moveSecurityRequirements(model);
            }

            if (renameHolder.hasRenames()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Updating references to renamed elements");
                }

                // Update references
                OpenAPIModelVisitor visitor = new RenameReferenceVisitor(renameHolder);
                OpenAPIModelWalker walker = new OpenAPIModelWalker(model);
                walker.accept(visitor);
            }

            // TODO: Generate an info block
            processedModels.add(model);
            includedProviders.add(provider);
        }

        if (!isServersIdentical(processedModels)) {
            for (OpenAPI model : processedModels) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Moving servers from the top level to under paths");
                }

                moveServersUnderPaths(model);
            }
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

        merged.setOpenapi("3.0.3");

        Info mergedInfo = OASFactory.createInfo();
        mergedInfo.setVersion("1.0");
        mergedInfo.setTitle("Merged documentation");
        merged.setInfo(mergedInfo);

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
    private boolean findPathClashes(OpenAPI model, OpenAPIProvider provider) {
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
            if (this.pathNames.contains(path)) {
                // Report a clash
                mergeProblems.add("Path " + path + " from " + provider + " clashes with a path from another module. " + provider + " will not be merged.");
                clashesFound = true;
            }
        }

        if (!clashesFound) {
            this.pathNames.addAll(pathItems.keySet());
        }

        return clashesFound;
    }

    private void renameClashingTags(OpenAPI document, RenameHolder renameHolder) {
        for (Tag tag : notNull(document.getTags())) {
            tag.setName(renameHolder.createUniqueName(NameType.TAG, tag.getName(), tag));
        }
    }

    private void renameClashingComponents(OpenAPI document, RenameHolder renameHolder) {
        Components components = document.getComponents();

        if (components == null) {
            return;
        }

        components.setCallbacks(renameComponents(NameType.CALLBACKS, components.getCallbacks(), renameHolder));
        components.setExamples(renameComponents(NameType.EXAMPLES, components.getExamples(), renameHolder));
        components.setHeaders(renameComponents(NameType.HEADERS, components.getHeaders(), renameHolder));
        components.setLinks(renameComponents(NameType.LINKS, components.getLinks(), renameHolder));
        components.setParameters(renameComponents(NameType.PARAMETERS, components.getParameters(), renameHolder));
        components.setRequestBodies(renameComponents(NameType.REQUEST_BODIES, components.getRequestBodies(), renameHolder));
        components.setResponses(renameComponents(NameType.RESPONSES, components.getResponses(), renameHolder));
        components.setSchemas(renameComponents(NameType.SCHEMAS, components.getSchemas(), renameHolder));
        components.setSecuritySchemes(renameComponents(NameType.SECURITY_SCHEMES, components.getSecuritySchemes(), renameHolder));
    }

    private <T> Map<String, T> renameComponents(NameType nameType, Map<String, T> componentMap, RenameHolder renameHolder) {
        if (componentMap == null) {
            return null;
        }

        HashMap<String, T> newMap = new HashMap<>();
        for (Entry<String, T> entry : componentMap.entrySet()) {
            String key = entry.getKey();
            newMap.put(renameHolder.createUniqueName(nameType, key, entry.getValue()), entry.getValue());
        }
        return newMap;
    }

    private void renameClashingOperationIds(OpenAPI document, RenameHolder renameHolder) {
        Paths paths = document.getPaths();
        if (paths == null) {
            return;
        }

        for (PathItem item : notNull(paths.getPathItems()).values()) {
            for (Operation op : notNull(item.getOperations()).values()) {
                op.setOperationId(renameHolder.createUniqueName(NameType.OPERATION_ID, op.getOperationId(), null));
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
    private static void prependPaths(OpenAPI model, String contextRoot, RenameHolder renameHolder) {
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
        // As a heuristic, if there were no servers, instead check whether all the paths appear to already start with the context root
        if (servers == null || servers.isEmpty()) {
            boolean allPathsStartWithContextRoot = true;
            for (String path : pathItems.keySet()) {
                if (!pathStartsWithContextRoot(path, contextRoot)) {
                    allPathsStartWithContextRoot = false;
                    break;
                }
            }
            if (allPathsStartWithContextRoot) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "No servers in model and all paths start with context root, assuming we shouldn't add the context root.");
                }
                allServersEndWithContextRoot = false;
            }
        }

        if (allServersEndWithContextRoot) {
            for (Entry<String, PathItem> entry : pathItems.entrySet()) {
                if (!pathServersEndWithContextRoot(entry.getValue(), contextRoot)) {
                    allServersEndWithContextRoot = false;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Server under " + entry.getKey() + " did not end with context root " + contextRoot);
                    }

                }
            }
        }

        if (allServersEndWithContextRoot) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removing context root " + contextRoot + " from servers and adding it to paths");
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
                renameHolder.registerRename(NameType.PATHS, entry.getKey(), newPath);
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

    private static boolean pathStartsWithContextRoot(String path, String contextRoot) {
        return path.startsWith(contextRoot);
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

    private static boolean isSecurityIdentical(List<OpenAPIProvider> documents) {
        List<List<SecurityRequirement>> reqs = documents.stream().map(d -> d.getModel().getSecurity()).collect(toList());

        return allEqual(reqs, (a, b) -> listsEqual(a, b, MergeProcessor::isSecurityRequirementsEqual));
    }

    private static boolean isSecurityRequirementsEqual(SecurityRequirement a, SecurityRequirement b) {
        return Objects.equals(a.getSchemes(), b.getSchemes());
    }

    private static boolean isServersIdentical(List<OpenAPI> processedModels) {
        List<List<Server>> servers = processedModels.stream().map(OpenAPI::getServers).collect(toList());
        return allEqual(servers, (a, b) -> listsEqual(a, b, MergeProcessor::isServersEqual));
    }

    private static boolean isServersEqual(Server a, Server b) {
        return Objects.equals(a.getUrl(), b.getUrl())
               && Objects.equals(a.getDescription(), b.getDescription())
               && Objects.equals(a.getExtensions(), b.getExtensions())
               && mapsEqual(a.getVariables(), b.getVariables(), MergeProcessor::isServerVariableEqual);
    }

    private static boolean isServerVariableEqual(ServerVariable a, ServerVariable b) {
        return listsEqual(a.getEnumeration(), b.getEnumeration(), Objects::equals)
               && Objects.equals(a.getDescription(), b.getDescription())
               && Objects.equals(a.getDefaultValue(), b.getDefaultValue());
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

    /**
     * Check whether all elements of {@code collection} are equal to each other using the given equality function
     * <p>
     * Actually assumes that equals is implemented properly and just checks that the first element is equal to all others
     * <p>
     * If {@code collection} contains less than two elements, this method will always return {@code true}.
     * 
     * @param <T> the element type
     * @param collection the collection of elements to test for equality
     * @param comparator the function to use to test equality
     * @return {@code true} if all elements of {@code collection} are equal, {@code false} otherwise
     */
    private static <T> boolean allEqual(Collection<? extends T> collection, BiPredicate<? super T, ? super T> comparator) {
        Iterator<? extends T> i = collection.iterator();
        if (!i.hasNext()) {
            return true;
        }

        T first = i.next();
        while (i.hasNext()) {
            if (!equals(first, i.next(), comparator)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether two lists are equal, using {@code comparator} to compare the list items
     * 
     * @param <T> the list item type
     * @param a the first list, may be {@code null}
     * @param b the second list, may be {@code null}
     * @param comparator the comparison function
     * @return {@code true} if {@code a} and {@code b} are equal, {@code false} otherwise
     */
    private static <T> boolean listsEqual(List<? extends T> a, List<? extends T> b, BiPredicate<? super T, ? super T> comparator) {
        if (a == null) {
            return b == null;
        } else {
            if (b == null) {
                return false;
            }
        }

        if (a.size() != b.size()) {
            return false;
        }

        for (int i = 0; i < a.size(); i++) {
            if (!equals(a.get(i), b.get(i), comparator)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether two maps are equal, using {@code comparator} to compare the map values
     * 
     * @param <T> the map item type
     * @param a the first map, may be {@code null}
     * @param b the second map, may be {@code null}
     * @param comparator the comparison function
     * @return {@code true} if {@code a} and {@code b} are equal, {@code false} otherwise
     */
    private static <T> boolean mapsEqual(Map<?, ? extends T> a, Map<?, ? extends T> b, BiPredicate<? super T, ? super T> comparator) {
        if (a == null) {
            return b == null;
        } else {
            if (b == null) {
                return false;
            }
        }

        if (!Objects.equals(a.entrySet(), b.entrySet())) {
            return false;
        }

        for (Entry<?, ? extends T> entry : a.entrySet()) {
            T itema = entry.getValue();
            T itemb = b.get(entry.getKey());
            if (!equals(itema, itemb, comparator)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if two objects are equal, using {@code comparator} to test their equality if both {@code a} and {@code b} are not {@code null}.
     * 
     * @param <T> the type of {@code a} and {@code b}
     * @param a the first object
     * @param b the second object
     * @param comparator the comparison function
     * @return {@code true} if {@code a} and {@code b} are equal, {@code false} otherwise
     */
    private static <T> boolean equals(T a, T b, BiPredicate<? super T, ? super T> comparator) {
        if (a == null) {
            if (b == null) {
                return true;
            } else {
                return false;
            }
        } else {
            if (b == null) {
                return false;
            } else {
                if (!comparator.test(a, b)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Converts {@code null} to an empty map
     * 
     * @param in a map, or {@code null}
     * @return an empty map if {@code in} is {@code null}, otherwise {@code in}
     */
    private static <K, V> Map<K, V> notNull(Map<K, V> in) {
        if (in == null) {
            return Collections.emptyMap();
        } else {
            return in;
        }
    }

    /**
     * Converts {@code null} to an empty list
     * 
     * @param in a list, or {@code null}
     * @return an empty list if {@code in} is {@code null}, otherwise {@code in}
     */
    private static <V> List<V> notNull(List<V> in) {
        if (in == null) {
            return Collections.emptyList();
        } else {
            return in;
        }
    }

    /**
     * Creates and stores new unique names for model elements.
     * <p>
     * Certain names must be unique within an OpenAPI document. When merging documents, these names may have to be changed so that they don't clash.
     * <p>
     * This class handles detecting clashes, creating new names as required and looking up the new names of renamed elements.
     */
    public class RenameHolder {
        private Map<NameType, Map<String, String>> renames = new HashMap<>();
        private boolean hasRenames = false;

        /**
         * Get the map of renames for the current model of the given name type
         * <p>
         * The returned map maps from the original non-unique name, to the new generated unique name
         * 
         * @param nameType the name type
         * @return the map of renames
         */
        @Trivial
        private Map<String, String> getRenameMap(NameType nameType) {
            return renames.computeIfAbsent(nameType, (k) -> new HashMap<>());
        }

        /**
         * Get the set of names and their values in use for the given name type
         * <p>
         * Note that this is shared between all models being merged.
         * 
         * @param nameType the name type
         * @return map of names to their values
         */
        @Trivial
        private Map<String, Object> getNamesInUse(NameType nameType) {
            return MergeProcessor.this.namesInUse.computeIfAbsent(nameType, (k) -> new HashMap<>());
        }
        
        private static final String NO_VALUE = "NO VALUE";

        /**
         * Create and reserve a unique name, based on a possibly non-unique name
         * <p>
         * If this method is called with the same {@code oldName} and different {@code value} when processing different documents, it will return different names.
         * <p>
         * However, if this method is called multiple times with the same {@code oldName} <i>within the same document</i> it will return the same name. This is to make it easier
         * to process tags which can be used without previous definition.
         * 
         * @param nameType the type of name
         * @param oldName the possibly non-unique name
         * @param value the value associated with the name, or {@code null} to not perform an equality check on the value
         * @return the new name (which may be the same as {@code oldName})
         */
        public String createUniqueName(NameType nameType, String oldName, Object value) {
            if (oldName == null) {
                return null;
            }

            Map<String, String> renameMap = getRenameMap(nameType);

            String previousRename = renameMap.get(oldName);
            if (previousRename != null) {
                return previousRename;
            }
            
            Map<String, Object> namesInUse = getNamesInUse(nameType);
            String newName = oldName;
            Object valueInUse = namesInUse.get(newName);
            if (valueInUse != null && (valueInUse == NO_VALUE || !ModelEquality.equals(valueInUse, value))) {
                // We need to rename
                int count = 1;
                newName = oldName + count;
                valueInUse = namesInUse.get(newName);
                
                // Compute the new name
                while (valueInUse != null && (valueInUse == NO_VALUE || !ModelEquality.equals(valueInUse, value))) {
                    count++;
                    newName = oldName + count;
                    valueInUse = namesInUse.get(newName);
                }
                
                // Store the rename
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Renamed " + nameType + " " + oldName + " -> " + newName);
                }
                hasRenames = true;
            }

            namesInUse.put(newName, value == null ? NO_VALUE : value);
            renameMap.put(oldName, newName);
            return newName;
        }

        /**
         * Look up the corresponding name to which {@code oldName} was renamed
         * <p>
         * If {@code oldName} was previously passed to {@link #createUniqueName(NameType, String)} or {@link #registerRename(NameType, String, String)}, then the new name will be
         * returned, otherwise {@code oldName} will be returned.
         * <p>
         * This is useful for updating references to objects which may have been renamed
         * 
         * @param nameType the type of name
         * @param oldName the possibly non-unique name
         * @return the corresponding new name (which may be the same as {@code oldName}
         */
        public String lookupName(NameType nameType, String oldName) {
            return getRenameMap(nameType).getOrDefault(oldName, oldName);
        }

        /**
         * Register that something is being renamed (for reasons other than it clashing with another name)
         * <p>
         * This is used e.g. when a path name is changed to add the context root.
         * <p>
         * In most other circumstances, {@link #createUniqueName(NameType, String)} should be used instead to generate a non-clashing name
         * 
         * @param nameType the type of name
         * @param oldName the old name
         * @param newName the new name
         */
        public void registerRename(NameType nameType, String oldName, String newName) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                if (!oldName.equals(newName)) {
                    Tr.event(tc, "Manual rename " + nameType + " " + oldName + " -> " + newName);
                }
            }

            hasRenames = true;
            getRenameMap(nameType).put(oldName, newName);
        }

        /**
         * Returns whether any renames have been recorded.
         * <p>
         * This will be true if a call to {@link #createUniqueName(NameType, String)} returned a different name to the one it was passed or if
         * {@link #registerRename(NameType, String, String)} has been called.
         * 
         * @return {@code true} if any renames have been recorded, otherwise {@code false}
         */
        public boolean hasRenames() {
            return hasRenames;
        }
    }

    public enum NameType {
        TAG,
        OPERATION_ID,
        CALLBACKS,
        EXAMPLES,
        HEADERS,
        LINKS,
        PARAMETERS,
        REQUEST_BODIES,
        RESPONSES,
        SCHEMAS,
        SECURITY_SCHEMES,
        PATHS
    }

}
