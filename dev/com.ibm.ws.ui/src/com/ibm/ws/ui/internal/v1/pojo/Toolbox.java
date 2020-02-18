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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ui.internal.RequestNLS;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.IToolbox;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.internal.validation.InvalidToolboxException;
import com.ibm.ws.ui.persistence.IPersistenceProvider;

/**
 * Class design requirements:
 * 1. JSONable POJO. We leverage Jackson to serialize this object into JSON format.
 * 2. Contains the tools for a single user.
 */
public class Toolbox implements IToolbox {
    private static transient final TraceComponent tc = Tr.register(Toolbox.class);

    public static transient final String PERSIST_NAME = "toolbox";
    private transient ICatalog catalog;
    private transient IPersistenceProvider persist;

    /**
     * Set in {@link #setUserId(String)}.
     * Not serialized by Jackson.
     */
    private transient String persistedName;

    /**
     * The userId which owns the toolbox.
     * <b>This is serialized by Jackson</b>
     */
    private String ownerId = null;

    /**
     * The owner's display name.
     * <b>This is serialized by Jackson</b>
     */
    private String ownerDisplayName = null;

    /**
     * The toolbox metadata. This is not directly set by external actions.
     * <b>This is serialized by Jackson</b>
     */
    private Map<String, Object> _metadata = null;

    /**
     * The toolbox preferences. This is not directly set by external actions.
     * <b>This is serialized by Jackson</b>
     */
    private Map<String, Object> preferences = null;

    /**
     * The set of tool entries contained within the toolbox.
     * <b>This is serialized by Jackson</b>
     */
    private List<ToolEntry> toolEntries = null;

    /**
     * The set of bookmarks contained within the toolbox.
     * <b>This is serialized by Jackson as a List</b> (see {@link #getBookmarks()}).
     */
    private Map<String, Bookmark> bookmarks = null;

    /**
     * Zero-argument constructor used by Jackson.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private Toolbox() {
        // No initialization of internal data structures as Jackson will set them via setters.
    }

    /**
     * @param requestername
     * @param toolbox
     */
    private void initializeDefaultToolbox() {
        // Create default tool order based on the catalog contents
        List<ToolEntry> toolEntries = new ArrayList<ToolEntry>();
        for (FeatureTool tool : catalog.getFeatureTools()) {
            toolEntries.add(new ToolEntry(tool.getId(), tool.getType()));
        }
        for (Bookmark tool : catalog.getBookmarks()) {
            toolEntries.add(new ToolEntry(tool.getId(), tool.getType()));
        }
        this.setToolEntries(toolEntries);
        this.setBookmarks(new ArrayList<Bookmark>());

        // add user id/display name etc
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(METADATA_IS_DEFAULT, true);
        map.put(METADATA_LAST_MODIFIED, new Date().getTime());
        this.set_metadata(map);

        this.setPreferences(Collections.<String, Object> emptyMap());
    }

    /**
     * 
     * @param catalog
     * @param persist
     * @param userId
     * @param userDisplayName
     */
    public Toolbox(final ICatalog catalog, final IPersistenceProvider persist, final String userId, final String userDisplayName) {
        setCatalog(catalog);
        setPersistenceProvider(persist);
        setOwnerId(userId);
        setOwnerDisplayName(userDisplayName);

        initializeDefaultToolbox();
    }

    /**
     * Sets the catalog instance.
     * Must be called if deserialized by Jackson.
     * 
     * @param catalog The singleton instance of ICatalog
     */
    public synchronized void setCatalog(final ICatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Sets the persistence provider instance.
     * Must be called if deserialized by Jackson.
     * 
     * @param persist The instance of IPersistenceProvider
     */
    public synchronized void setPersistenceProvider(final IPersistenceProvider persist) {
        this.persist = persist;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private synchronized void setOwnerId(final String userId) {
        this.ownerId = userId;
        this.persistedName = PERSIST_NAME + "-" + userId;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private synchronized void setOwnerDisplayName(final String userDisplayName) {
        this.ownerDisplayName = userDisplayName;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public synchronized String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private synchronized void set_metadata(final Map<String, Object> _metadata) {
        this._metadata = _metadata;
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized Map<String, Object> get_metadata() {
        if (_metadata == null) {
            return null;
        }
        Map<String, Object> returnedMap = new HashMap<String, Object>();
        returnedMap.putAll(_metadata);
        return Collections.unmodifiableMap(returnedMap);
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private synchronized void setPreferences(final Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized Map<String, Object> getPreferences() {
        return Collections.unmodifiableMap(preferences);
    }

    /**
     * Checks if the field (key or value) contains malicious content.
     * 
     * @param field
     * @return {@code true} if the field contains XSS, or {@code false} otherwise.
     */
    private boolean containsXSS(Object field) {
        if (field instanceof String) {
            String toValidate = (String) field;
            final String lower = toValidate.toLowerCase();
            if (lower.indexOf("<script") != -1 || lower.indexOf("<img") != -1 || lower.indexOf("<iframe") != -1) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Validates all keys and values of the preferences map are non-malicious.
     * 
     * @param preferences
     */
    private void validatePreferences(Map<String, Object> preferences) {
        List<String> keysToRemove = new ArrayList<String>();
        for (Entry<String, Object> entry : preferences.entrySet()) {
            if (containsXSS(entry.getKey()) || containsXSS(entry.getValue())) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "The preferences entry '" + entry.getKey() + "' contains malicious content. Removing this entry from preferences.");
                }
                // Remove key
                keysToRemove.add(entry.getKey());
            }
        }
        for (String key : keysToRemove) {
            preferences.remove(key);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Map<String, Object> updatePreferences(Map<String, Object> preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("updatePreferences requires a non-null map");
        }
        validatePreferences(preferences);
        Map<String, Object> oldPrefs = this.preferences;
        this.preferences = preferences;

        updateMetadataOnChange();
        storeToolbox();

        return oldPrefs;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @Trivial
    private synchronized void setToolEntries(final List<ToolEntry> toolEntries) {
        this.toolEntries = toolEntries;
    }

    /** {@inheritDoc} */
    @Trivial
    @Override
    public synchronized List<ToolEntry> getToolEntries() {
        if (toolEntries == null) {
            return null;
        }
        List<ToolEntry> returnedList = new ArrayList<ToolEntry>();
        returnedList.addAll(toolEntries);
        return Collections.unmodifiableList(returnedList);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void updateToolEntries(final List<ToolEntry> newToolEntries) throws IllegalArgumentException, NoSuchToolException {
        if (this.toolEntries == null ||
            newToolEntries == null ||
            (this.toolEntries.size() != newToolEntries.size())) {
            throw new IllegalArgumentException(RequestNLS.formatMessage(tc, "TOOL_LIST_NOT_MATCH_TOOLBOX",
                                                                        newToolEntries != null ? newToolEntries.size() : 0,
                                                                        this.toolEntries != null ? this.toolEntries.size() : 0));
        }

        Map<ToolEntry, Integer> occurrencies = new HashMap<ToolEntry, Integer>();

        // validate the tool list against the current toolbox
        for (ToolEntry newTool : newToolEntries) {
            boolean found = this.toolEntries.contains(newTool);
            // checking for duplicate tool entry
            occurrencies.put(newTool,
                             occurrencies.containsKey(newTool) ? occurrencies.get(newTool) + 1 : 1);

            if (!found) {
                throw new NoSuchToolException(RequestNLS.formatMessage(tc, "TOOL_NOT_FOUND_FROM_TOOLBOX", newTool.getId(), newTool.getType()));
            }
        }

        for (Entry<ToolEntry, Integer> entry : occurrencies.entrySet()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "ToolEntry: ", entry.getKey() + ", occurences: " + entry.getValue());
            }
            if (entry.getValue() > 1) {
                throw new IllegalArgumentException(RequestNLS.formatMessage(tc, "TOOL_LIST_DUPLICATE_TOOL", entry.getKey().getId()));
            }
        }

        this.toolEntries = newToolEntries;
        updateMetadataOnChange();
        storeToolbox();
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
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
    public synchronized ITool getToolEntry(final String id) {
        ITool tool = bookmarks.get(id);
        if (tool != null) {
            return tool;
        }
        // If not in our tools AND its in our tool entries, check the catalog
        for (ToolEntry e : toolEntries) {
            if (e.getId().equals(id)) {
                return catalog.getTool(id);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Bookmark getBookmark(String id) {
        return bookmarks.get(id);
    }

    /**
     * Updates the metadata when a change is made to the contents of the toolbox.
     */
    private synchronized void updateMetadataOnChange() {
        _metadata.put(METADATA_LAST_MODIFIED, new Date().getTime());
        _metadata.put(METADATA_IS_DEFAULT, false);
    }

    /**
     * Attempts to store the toolbox to the persistence layer.
     * <p>
     * This method will not fail, but will print an error if persisting the
     * toolbox encounters an error. FFDCs are logged as we do not expect any
     * problems in normal flows.
     */
    synchronized void storeToolbox() {
        try {
            persist.store(persistedName, this);
        } catch (JSONMarshallException e) {
            // This should not occur. FFDC here.
            Tr.error(tc, "UNABLE_TO_PROMOTE_TOOL_JSON_DATA_CONTENT", new Object[] { ownerId, e.getMessage() });
        } catch (IOException e) {
            // This should not occur. FFDC here.
            Tr.error(tc, "STORE_TOOLBOX_TO_PERSISTENCE_ERROR", new Object[] { ownerId, e.getMessage() });
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ToolEntry addToolEntry(final ToolEntry tool) throws NoSuchToolException, DuplicateException, InvalidToolException {
        ToolEntry toAdd = tool;
        try {
            toAdd.validateSelf();
        } catch (InvalidToolException e) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "TOOL_OBJECT_NOT_VALID_TOOLBOX", e.getMessage()));
        }
        for (ToolEntry toolEntry : toolEntries) {
            if (toolEntry.getId().equals(toAdd.getId())) {
                throw new DuplicateException(RequestNLS.formatMessage(tc, "TOOL_ALREADY_EXIST_TOOLBOX", toAdd.getId()));
            }
        }
        ITool catalogEntry = catalog.getTool(toAdd.getId());
        if (catalogEntry == null) {
            throw new NoSuchToolException(RequestNLS.formatMessage(tc, "TOOL_NOT_FOUND", toAdd.getId()));
        } else {
            if (!catalogEntry.getType().equals(toAdd.getType())) {
                throw new NoSuchToolException(RequestNLS.formatMessage(tc, "TOOL_AND_TYPE_NOT_FOUND", toAdd.getId(), toAdd.getType()));
            }
        }
        toolEntries.add(toAdd);
        updateMetadataOnChange();
        storeToolbox();
        return toAdd;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Bookmark addBookmark(final Bookmark tool) throws DuplicateException, InvalidToolException {
        Bookmark toAdd = tool;
        // If the id and type are to be implied, imply them 
        if (tool.getId() == null && tool.getType() == null) {
            toAdd = new Bookmark(tool.getName(), tool.getURL(), tool.getIcon(), tool.getDescription());
        }
        try {
            toAdd.validateSelf();
        } catch (InvalidToolException e) {
            throw new InvalidToolException(RequestNLS.formatMessage(tc, "TOOL_OBJECT_NOT_VALID_TOOLBOX", e.getMessage()));
        }
        if (bookmarks.get(toAdd.getId()) != null) {
            throw new DuplicateException(RequestNLS.formatMessage(tc, "TOOL_ALREADY_EXIST_TOOLBOX", toAdd.getId()));
        }
        bookmarks.put(toAdd.getId(), toAdd);
        toolEntries.add(new ToolEntry(toAdd.getId(), toAdd.getType()));
        updateMetadataOnChange();
        storeToolbox();

        return toAdd;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void reset() {
        if (tc.isEventEnabled()) {
            Tr.event(tc, this.toString() + " reset request received. Resetting toolbox to initial state.");
        }

        initializeDefaultToolbox();
        storeToolbox();

        if (tc.isEventEnabled()) {
            Tr.event(tc, this.toString() + " reset completed.");
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ITool deleteToolEntry(final String id)
    {
        ToolEntry deletedTool = null;
        for (Iterator<ToolEntry> iterator = toolEntries.iterator(); iterator.hasNext() && deletedTool == null;) {
            ToolEntry tool = iterator.next();
            if (tool.getId().equals(id)) {
                if (ITool.TYPE_BOOKMARK.equals(tool.getType())) {
                    bookmarks.remove(id);
                }
                deletedTool = tool;
                iterator.remove();
                updateMetadataOnChange();
                storeToolbox();
                break;
            }
        }
        return deletedTool;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Bookmark deleteBookmark(final String id)
    {
        Bookmark deletedBookmark = bookmarks.remove(id);
        if (deletedBookmark != null) {
            for (Iterator<ToolEntry> iterator = toolEntries.iterator(); iterator.hasNext();) {
                ToolEntry tool = iterator.next();
                if (tool.getId().equals(id)) {
                    iterator.remove();
                    updateMetadataOnChange();
                    storeToolbox();
                    break;
                }
            }
        }
        return deletedBookmark;
    }

    /**
     * Checks the Catalog to see if the tool exists.
     * 
     * @param toolsToBeRemoved
     * @param tool
     */
    private void checkCatalog(List<ToolEntry> toolsToBeRemoved, ToolEntry tool) {
        if (catalog.getTool(tool.getId()) == null) {
            Tr.warning(tc, "TOOLBOX_TOOL_NO_LONGER_AVAILABLE", new Object[] { tool.getId(), ownerId });
            toolsToBeRemoved.add(tool);
        }
    }

    /**
     * Validates the Toolbox is complete. An incomplete Toolbox would
     * be one which has null tools or missing metadata.
     * 
     * @return {@code true} if the Toolbox is valid, or {@code false} if required fields are missing
     */
    @Override
    public synchronized void validateSelf() throws InvalidToolboxException {
        if (ownerId == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required 'ownerId' field.");
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the 'ownerId' field is null");
        }
        if (ownerDisplayName == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required 'ownerDisplayName' field.");
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the 'ownerDisplayName' field is null");
        }
        if (toolEntries == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required 'toolEntries' field.");
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the 'toolEntries' field is null");
        }
        if (bookmarks == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required 'bookmarks' field.");
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the 'bookmarks' field is null");
        }
        if (preferences == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required 'preferences' field.");
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the 'preferences' field is null");
        }
        if (_metadata == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required '_metadata' field.");
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the '_metadata' field is null");
        }

        if (!_metadata.containsKey(METADATA_LAST_MODIFIED)) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required metadata '" + METADATA_LAST_MODIFIED + "'.", _metadata);
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the metadata '" + METADATA_LAST_MODIFIED + "' is not defined");
        }
        if (!_metadata.containsKey(METADATA_IS_DEFAULT)) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "The Toolbox is not considered valid because it is missing its required metadata '" + METADATA_IS_DEFAULT + "'.", _metadata);
            }
            throw new InvalidToolboxException("The Toolbox is not valid, the metadata '" + METADATA_IS_DEFAULT + "' is not defined");
        }

        validatePreferences(preferences);

        // validate the URL map
        List<String> bookmarksToBeRemoved = new ArrayList<String>();
        for (Entry<String, Bookmark> entry : bookmarks.entrySet()) {
            String key = entry.getKey();
            Bookmark url = entry.getValue();
            try {
                url.validateSelf();
                boolean found = false;
                for (ToolEntry tool : toolEntries) {
                    if (tool.getId().equals(key)) {
                        found = true;
                    }
                }
                if (!found) {
                    bookmarksToBeRemoved.add(key);
                }
            } catch (InvalidToolException e) {
                bookmarksToBeRemoved.add(key);
            }
        }
        for (String key : bookmarksToBeRemoved) {
            bookmarks.remove(key);
        }

        // validate each tool entry
        List<ToolEntry> toolsToBeRemoved = new ArrayList<ToolEntry>();
        for (ToolEntry tool : toolEntries) {
            try {
                tool.validateSelf();
                if (bookmarksToBeRemoved.contains(tool.getId())) {
                    toolsToBeRemoved.add(tool);
                } else if (ITool.TYPE_BOOKMARK.equalsIgnoreCase(tool.getType())) {
                    if (bookmarks.get(tool.getId()) == null) {
                        checkCatalog(toolsToBeRemoved, tool);
                    }
                } else {
                    checkCatalog(toolsToBeRemoved, tool);
                }
            } catch (InvalidToolException invalidToolException) {
                // This should never happen. FFDC when it does.
                Tr.warning(tc, "INVALID_TOOL_CONTENT_WARNING_TOOLBOX", new Object[] { tool.getId(), ownerId, invalidToolException.getMessage() });
                toolsToBeRemoved.add(tool);
            }
        }
        if (toolsToBeRemoved.size() > 0) {
            toolEntries.removeAll(toolsToBeRemoved);
        }

        // Lastly, if we made changes, update
        if (toolsToBeRemoved.size() > 0 || bookmarksToBeRemoved.size() > 0) {
            updateMetadataOnChange();
        }
    }

    /**
     * Selectively compare the contents of the metdata. Ignored metadata: {@link IToolbox#METADATA_LAST_MODIFIED}.
     * 
     * @param that
     * @return
     */
    private boolean equalsMetadata(final Toolbox that) {
        synchronized (this) {
            synchronized (that) {
                if (this._metadata == that._metadata)
                    return true;

                boolean matches = true;
                if (this._metadata != null && that._metadata != null) {
                    for (Entry<String, Object> thisEntry : this._metadata.entrySet()) {
                        String key = thisEntry.getKey();
                        Object value = thisEntry.getValue();
                        if ((value == null) && (that._metadata.get(key) != null)) {
                            return false;
                        } else if (!key.equals(METADATA_LAST_MODIFIED)) {
                            matches &= that._metadata.get(key).equals(value);
                        }

                    }
                }
                return matches;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Toolbox) {
            Toolbox that = (Toolbox) o;
            synchronized (this) {
                synchronized (that) {
                    boolean sameFields = true;
                    sameFields &= (this.ownerId == that.ownerId) || (this.ownerId != null && this.ownerId.equals(that.ownerId));
                    sameFields &= (this.ownerDisplayName == that.ownerDisplayName) || (this.ownerDisplayName != null && this.ownerDisplayName.equals(that.ownerDisplayName));
                    sameFields &= (this.preferences == that.preferences) || (this.preferences != null && this.preferences.equals(that.preferences));
                    sameFields &= (this.toolEntries == that.toolEntries) || (this.toolEntries != null && this.toolEntries.equals(that.toolEntries));
                    sameFields &= (this.bookmarks == that.bookmarks) || (this.bookmarks != null && this.bookmarks.equals(that.bookmarks));
                    return sameFields && equalsMetadata(that);
                }
            }
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}<p>
     * A toolbox with no tools is not really likely.
     * Therefore we don't really need to worry about the hash code.
     */
    @Override
    public synchronized int hashCode() {
        return (toolEntries == null) ? 0 : toolEntries.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Toolbox");
        if (ownerId != null) {
            sb.append('(');
            sb.append(ownerId);
            sb.append(')');
        }
        return sb.toString();
    }

}
