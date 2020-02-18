/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.IFeatureToolService;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolboxService;
import com.ibm.ws.ui.internal.validation.InvalidCatalogException;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.IPersistenceProvider;

/**
 * Class design requirements:
 * 1. JSONable POJO. We leverage Jackson to serialize this object into JSON format.
 * 2. Container of all system tool configuration data.
 * 3. Single instance per JVM. Must be treated as a singleton.
 */
public class Catalog implements ICatalog {
    private static transient final TraceComponent tc = Tr.register(Catalog.class);

    public static transient final String PERSIST_NAME = "catalog";
    private transient IPersistenceProvider persist;
    private transient IToolboxService toolboxes;
    private transient IFeatureToolService featureToolService;

    /**
     * The catalog metadata. This is not directly set by external actions.
     * <b>This is serialized by Jackson</b>
     */
    private Map<String, Object> _metadata = null;

    /**
     * The set of feature tools contained within the catalog.
     * <b>This is serialized by Jackson as a List</b> (see {@link #getFeatureTools()}).
     * This list's contents are based on the JVM's Locale.
     */
    private Map<String, FeatureTool> featureTools = null;

    /**
     * The set of bookmarks contained within the catalog.
     * <b>This is serialized by Jackson as a List</b> (see {@link #getBookmarks()}).
     */
    private Map<String, Bookmark> bookmarks = null;

    /**
     * Initializes or resets the catalog to default content.
     */
    private void initializeDefaults()
    {
        featureTools = new HashMap<String, FeatureTool>();
        bookmarks = new HashMap<String, Bookmark>();
        // Add a bookmark for wasdev.net
        Bookmark wasdev = new Bookmark("wasdev.net", "http://wasdev.net", "images/tools/wasdev_142x142.png", "The WASdev community site.");
        bookmarks.put(wasdev.getId(), wasdev);
        _metadata = new HashMap<String, Object>();
        _metadata.put(METADATA_LAST_MODIFIED, new Date().getTime());
        _metadata.put(METADATA_IS_DEFAULT, true);
    }

    /**
     * Default catalog constructor. Can build a default set of tools.
     * Zero-argument constructor used by Jackson.
     * Intentionally private visibility.
     */
    Catalog() {}

    /**
     * Construct a new, default instance of the Catalog.
     * 
     * @param persist
     * @param toolboxService
     */
    public Catalog(final IPersistenceProvider persist, final IToolboxService toolboxService, final IFeatureToolService featureToolService) {
        setIPersistenceProvider(persist);
        setIToolboxService(toolboxService);
        setIFeatureToolService(featureToolService);

        initializeDefaults();
    }

    /**
     * Sets the persistence provider instance.
     * Must be called if deserialized by Jackson.
     * 
     * @param catalog The instance of IPersistenceProvider
     */
    @Trivial
    public synchronized void setIPersistenceProvider(final IPersistenceProvider persist) {
        this.persist = persist;
    }

    /**
     * Sets the toolbox service instance.
     * Must be called if deserialized by Jackson.
     * 
     * @param catalog The singleton instance of IToolboxService
     */
    @Trivial
    public synchronized void setIToolboxService(final IToolboxService toolboxes) {
        this.toolboxes = toolboxes;
    }

    /**
     * Sets the IFeatureToolService instance.
     * Must be called if deserialized by Jackson.
     * 
     * @param catalog The instance of IFeatureToolService
     */
    @Trivial
    public synchronized void setIFeatureToolService(IFeatureToolService featureToolService) {
        this.featureToolService = featureToolService;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private synchronized void set_metadata(final Map<String, Object> _metadata) {
        this._metadata = _metadata;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Map<String, Object> get_metadata() {
        if (_metadata == null) {
            return null;
        }
        Map<String, Object> returnedMap = new HashMap<String, Object>();
        // Ensure we're up-to-date with the tools on the system. Running getTools refreshes the tools.
        getFeatureTools();
        returnedMap.putAll(_metadata);
        return Collections.unmodifiableMap(returnedMap);
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private synchronized void setFeatureTools(final List<FeatureTool> featureTools) {
        Map<String, FeatureTool> newFeatureTools = null;
        if (featureTools != null) {
            newFeatureTools = new HashMap<String, FeatureTool>();
            for (FeatureTool tool : featureTools) {
                newFeatureTools.put(tool.getId(), tool);
            }

        }
        this.featureTools = newFeatureTools;
    }

    /**
     * This method adds any new tool feature manifests to the AdminCenter Catalog.
     * We build up the tool with the metadata in the feature manifest and in
     * some cases, metadata in the included Bundle manifests.
     * 
     * This method checks that each tool that has been installed via a feature,
     * is still valid, by ensuring that it is listed in the installedFeatures
     * list. If it isn't, then we remove it from the catalog.
     */
    private void validateToolsAgainstFeatures(boolean changeDefault) {
        // We can not bomb if we've not been validated.
        // If we don't have a featureToolService, log it and return
        if (featureToolService == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Catalog featureToolService has not been set. Returning without processing feature tools...");
            }
            return;
        }

        final Set<FeatureTool> featureSet = featureToolService.getTools();
        boolean metaDataChanged = false;

        // First, remove any features which are no longer valid. The tool could
        // no longer valid because the ID is no longer present, OR the tool's
        // data was changed (i.e. the feature name and version did not change,
        // but the tool URL is different).
        // We need to store the Tools away rather than deleting them within the
        // loop, because we'll get CurrentModificationExceptions.
        Set<String> toolsToDelete = new HashSet<String>();
        for (Entry<String, FeatureTool> entry : featureTools.entrySet()) {
            final String key = entry.getKey();
            final FeatureTool tool = entry.getValue();
            if (!featureSet.contains(tool)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "The previously stored FeatureTool " + tool + " is no longer valid. Adding to list of tools to delete.");
                toolsToDelete.add(key);
            }
        }

        // Now delete all of the tools that are no longer installed or are different.
        for (String toolToDelete : toolsToDelete) {
            featureTools.remove(toolToDelete);
            metaDataChanged = true;
        }

        // Now that we've removed dead or changed tools, add in the new tools
        for (FeatureTool featureTool : featureSet) {
            if (featureTools.get(featureTool.getId()) == null) {
                // If this tool isn't one of our existing tools, then add it.
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Adding FeatureTool to list of catalog tools: " + featureTool);
                }
                featureTools.put(featureTool.getId(), featureTool);
                metaDataChanged = true;
            }
        }

        // If the metadata has changed then we need to store the catalog and refresh the metadata.
        if (metaDataChanged) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Refreshing catalog metadata and persisting");
            }
            updateMetadataOnChange(changeDefault);
            if (changeDefault) {
                storeCatalog();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<FeatureTool> getFeatureTools() {
        if (featureTools == null) {
            return null;
        }

        // Ensure we're up-to-date with the tools on the system.
        validateToolsAgainstFeatures(true);

        if (featureToolService != null) {
            return Collections.unmodifiableList(featureToolService.getToolsForRequestLocale());
        } else {
            List<FeatureTool> ret = new ArrayList<FeatureTool>();
            ret.addAll(featureTools.values());
            return Collections.unmodifiableList(ret);
        }
    }

    /**
     * This method adds any new tool feature manifests to the AdminCenter Catalog.
     * We build up the tool with the metadata in the feature manifest and in
     * some cases, metadata in the included Bundle manifests.
     * 
     * This method checks that each tool that has been installed via a feature,
     * is still valid, by ensuring that it is listed in the installedFeatures
     * list. If it isn't, then we remove it from the catalog.
     */
    void initialProcessFeatures() {
        validateToolsAgainstFeatures(false);
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private synchronized void setBookmarks(final List<Bookmark> bookmarks) {
        Map<String, Bookmark> newBookmarks = null;
        if (bookmarks != null) {
            newBookmarks = new HashMap<String, Bookmark>();
            for (Bookmark tool : bookmarks) {
                newBookmarks.put(tool.getId(), tool);
            }

        }
        this.bookmarks = newBookmarks;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized List<Bookmark> getBookmarks() {
        if (bookmarks == null) {
            return null;
        }
        List<Bookmark> ret = new ArrayList<Bookmark>();
        ret.addAll(bookmarks.values());
        return Collections.unmodifiableList(ret);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ITool getTool(final String id) {
        ITool t = getFeatureTool(id);
        if (t != null) {
            return t;
        }
        return bookmarks.get(id);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized FeatureTool getFeatureTool(final String id) {
        if (featureToolService != null) {
            return featureToolService.getToolForRequestLocale(id);
        } else {
            return featureTools.get(id);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Bookmark getBookmark(final String id) {
        return bookmarks.get(id);
    }

    /**
     * Attempts to store the catalog to the persistence layer.
     * <p>
     * This method will not fail, but will print an error if persisting the
     * catalog encounters an error. FFDCs are logged as we do not expect any
     * problems in normal flows.
     */
    synchronized void storeCatalog() {
        try {
            persist.store(PERSIST_NAME, this);
        } catch (JSONMarshallException e) {
            // This should not occur. FFDC here.
            Tr.error(tc, "STORE_CATALOG_TO_MARSHALL_ERROR", e.getMessage());
        } catch (IOException e) {
            // This should not occur. FFDC here.
            Tr.error(tc, "STORE_CATALOG_TO_PERSISTENCE_ERROR", e.getMessage());
        }
    }

    /**
     * Updates the metadata when a change is made to the contents of the catalog.
     */
    private synchronized void updateMetadataOnChange(boolean changeDefault) {
        _metadata.put(METADATA_LAST_MODIFIED, new Date().getTime());
        if (changeDefault) {
            _metadata.put(METADATA_IS_DEFAULT, false);
        }
    }

    /** {@inheritDoc} */
    @FFDCIgnore(InvalidToolException.class)
    @Override
    public synchronized Bookmark addBookmark(final Bookmark tool) throws DuplicateException, InvalidToolException {
        Bookmark toAdd = tool;
        // If the id and type are to be implied, imply them 
        if (toAdd.getId() == null && toAdd.getType() == null) {
            toAdd = new Bookmark(tool.getName(), tool.getURL(), tool.getIcon(), tool.getDescription());
        }
        try {
            toAdd.validateSelf();
        } catch (InvalidToolException e) {
            // This can occur and is expected. Do not FFDC
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "TOOL_OBJECT_NOT_VALID", e.getMessage()));
        }
        if (bookmarks.get(toAdd.getId()) != null) {
            throw new DuplicateException(RequestNLS.formatMessage(tc, "TOOL_ALREADY_EXIST", toAdd.getId()));
        }
        bookmarks.put(toAdd.getId(), toAdd);
        updateMetadataOnChange(true);
        storeCatalog();
        return toAdd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Bookmark deleteBookmark(final String id) {
        Bookmark deletedTool = bookmarks.remove(id);
        if (deletedTool != null) {
            updateMetadataOnChange(true);
            storeCatalog();

            // Remove this tool from all of the toolboxes
            // A tool removed from the catalog is 'no longer available'
            toolboxes.removeToolEntryFromAllToolboxes(id);
        }
        return deletedTool;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void reset() {
        if (tc.isEventEnabled()) {
            Tr.event(tc, "Catalog reset request received. Removing all known catalog entries from all toolboxes.");
        }
        for (ITool t : featureTools.values()) {
            toolboxes.removeToolEntryFromAllToolboxes(t.getId());
        }
        for (ITool t : bookmarks.values()) {
            toolboxes.removeToolEntryFromAllToolboxes(t.getId());
        }

        if (tc.isEventEnabled()) {
            Tr.event(tc, "Catalog reset request received. Resetting catalog to initial state.");
        }
        initializeDefaults();
        initialProcessFeatures();
        storeCatalog();

        if (tc.isEventEnabled()) {
            Tr.event(tc, "Catalog reset complete.");
        }
    }

    /**
     * Validates the Catalog is complete. An incomplete Catalog would
     * be one which has null tools or missing metadata.
     * 
     * @return {@code true} if the Catalog is valid, or {@code false} if required fields are missing
     */
    @FFDCIgnore(InvalidToolException.class)
    @Override
    public synchronized void validateSelf() throws InvalidCatalogException {
        if (featureTools == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Catalog is not considered valid because it is missing its required 'featureTools' field.");
            }
            throw new InvalidCatalogException("The Catalog is not valid, the 'featureTools' field is null");
        }

        if (bookmarks == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Catalog is not considered valid because it is missing its required 'bookmarks' field.");
            }
            throw new InvalidCatalogException("The Catalog is not valid, the 'bookmarks' field is null");
        }

        if (_metadata == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Catalog is not considered valid because it is missing its required '_metadata' field.");
            }
            throw new InvalidCatalogException("The Catalog is not valid, the '_metadata' field is null");
        }

        if (!_metadata.containsKey(METADATA_LAST_MODIFIED)) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Catalog is not considered valid because it is missing its required metadata '" + METADATA_LAST_MODIFIED + "'.", _metadata);
            }
            throw new InvalidCatalogException("The Catalog is not valid, the metadata '" + METADATA_LAST_MODIFIED + "' is not defined");
        }
        if (!_metadata.containsKey(METADATA_IS_DEFAULT)) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Catalog is not considered valid because it is missing its required metadata '" + METADATA_IS_DEFAULT + "'.", _metadata);
            }
            throw new InvalidCatalogException("The Catalog is not valid, the metadata '" + METADATA_IS_DEFAULT + "' is not defined");
        }

        // validate each tool
        List<String> keyRemovals = new ArrayList<String>();
        for (Entry<String, FeatureTool> tool : featureTools.entrySet()) {
            try {
                tool.getValue().validateSelf();
            } catch (InvalidToolException invalidToolException) {
                // This can occur and is expected. Do not FFDC
                Tr.warning(tc, "INVALID_TOOL_CONTENT_WARNING", new Object[] { tool.getValue().getId(), invalidToolException.getMessage() });
                keyRemovals.add(tool.getKey());
            }
        }
        for (String key : keyRemovals) {
            featureTools.remove(key);
        }

        keyRemovals.clear();
        for (Entry<String, Bookmark> tool : bookmarks.entrySet()) {
            try {
                tool.getValue().validateSelf();
            } catch (InvalidToolException invalidToolException) {
                // This can occur and is expected. Do not FFDC
                Tr.warning(tc, "INVALID_TOOL_CONTENT_WARNING", new Object[] { tool.getValue().getId(), invalidToolException.getMessage() });
                keyRemovals.add(tool.getKey());
            }
        }
        for (String key : keyRemovals) {
            bookmarks.remove(key);
        }

        updateMetadataOnChange(true);
    }

    /**
     * {@inheritDoc} <p>
     * Comparison intentionally does not compare metadata.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Catalog) {
            Catalog that = (Catalog) o;
            synchronized (this) {
                synchronized (that) {
                    boolean sameFields = true;
                    sameFields &= (this.featureTools == that.featureTools) || (this.featureTools != null && (this.featureTools.equals(that.featureTools)));
                    sameFields &= (this.bookmarks == that.bookmarks) || (this.bookmarks != null && (this.bookmarks.equals(that.bookmarks)));
                    return sameFields;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}<p>
     * A catalog with no tools is not really likely.
     * Therefore we don't really need to worry about the hash code.
     */
    @Override
    public synchronized int hashCode() {
        return (featureTools == null) ? 0 : featureTools.hashCode();
    }
}
