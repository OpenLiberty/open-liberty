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
package com.ibm.ws.ui.internal.v1;

import java.util.List;
import java.util.Map;

import com.ibm.ws.ui.internal.v1.pojo.Bookmark;
import com.ibm.ws.ui.internal.v1.pojo.DuplicateException;
import com.ibm.ws.ui.internal.v1.pojo.NoSuchToolException;
import com.ibm.ws.ui.internal.v1.pojo.ToolEntry;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * The toolbox is the a collection of selected tools for use by UI users.
 * Each user has its own toolbox.
 * 
 * The information about the tools stored in the toolbox is user based.
 */
public interface IToolbox extends SelfValidatingPOJO {

    /**
     * Metadata field (Long): the last time the toolbox contents were modified.
     */
    String METADATA_LAST_MODIFIED = "lastModified";

    /**
     * Metadata field (Boolean): represents the toolbox is the default toolbox
     * and has never been modified.
     */
    String METADATA_IS_DEFAULT = "isDefault";

    /**
     * Preference field (Boolean): indicates if the toolbox user preference for
     * bidi has been enabled. Default is false.
     */
    String PREFERENCE_BIDI_ENABLED = "bidiEnabled";

    /**
     * Preference field (String): represents the bidi preference for the text
     * direction for text entry fields. Default is ltr (left-to-right). This
     * value is only honored when bidi is enabled.
     */
    String PREFERENCE_BIDI_TEXT_DIRECTION = "bidiTextDirection";

    /**
     * Preference value (String): represents left-to-right ordering setting for the
     * text entry fields.
     */
    String PREFERENCE_BIDI_TEXT_DIRECTION_LTR = "ltr";

    /**
     * Preference value (String): represents right-to-left ordering setting for the
     * text entry fields.
     */
    String PREFERENCE_BIDI_TEXT_DIRECTION_RTL = "rtl";

    /**
     * Preference value (String): represents context ordering setting for the
     * text entry fields.
     */
    String PREFERENCE_BIDI_TEXT_DIRECTION_CONTEXTUAL = "contextual";

    /**
     * Retrieve the toolbox owner's user ID.
     * 
     * @return the toolbox owner's user ID.
     */
    String getOwnerId();

    /**
     * Retrieve the toolbox owner's user display name.
     * 
     * @return the toolbox owner's user display name.
     */
    String getOwnerDisplayName();

    /**
     * Retrieve the toolbox metadata. The returned Map is a read-only
     * snapshot of the metadata at the time it was asked for.
     * 
     * <p>See the METADATA_* fields defined in this interface.</p>
     * 
     * 
     * @return A Map of all of the toolbox metadata
     */
    Map<String, Object> get_metadata();

    /**
     * Retrieve the toolbox preferences. The returned Map is a read-only
     * snapshot of the preferences at the time it was asked for.
     * 
     * <p>See the PREFERENCE_* fields defined in this interface for known values.</p>
     * 
     * @return A Map of all of the toolbox preferences
     */
    Map<String, Object> getPreferences();

    /**
     * Updates the toolbox preferences. The preferences Map is a collection of
     * known and unknown key/value pairs.
     * 
     * <p>See the PREFERENCE_* fields defined in this interface for known values.</p>
     * 
     * @param preferences A Map of all of the toolbox preferences. Must not be {@code null}.
     * @param The previous Map of preferences
     */
    Map<String, Object> updatePreferences(Map<String, Object> preferences);

    /**
     * Retrieve the ordered list of the tool entries in the toolbox.
     * The returned List is a read-only snapshot of the order of the tools in
     * the toolbox at the time it was asked for.
     * 
     * @return The ordered List of all ToolEntry objects in the toolbox, {@code null} is not returned.
     */
    List<ToolEntry> getToolEntries();

    /**
     * Update the list of the tool entries in the toolbox.
     * 
     * @param the list of the tool entries to be update in the toolbox
     * @throws IllegalArgumentException If the list of tool entries size does not match the current toolbox size
     *             or if the list of tool entries to be update contains duplicate entries
     * @throws NoSuchToolException If a ToolEntry does not represent a matching tool in the toolbox
     */
    void updateToolEntries(List<ToolEntry> toolEntries) throws IllegalArgumentException, NoSuchToolException;

    /**
     * Retrieve all of the bookmarks in the toolbox.
     * The returned List is a read-only snapshot of the set of bookmarks in the
     * toolbox at the time it was asked for.
     * 
     * @return The List of all Bookmark objects in the toolbox, {@code null} is not returned.
     */
    List<Bookmark> getBookmarks();

    /**
     * Retrieve a tool by its ID.
     * <p>
     * The backing tool object for the specified ID is returned. This means if
     * the id represents a Bookmark or Feature tool, that object is returned.
     * 
     * @param id The tool ID
     * @return The Tool object with the specified ID, or {@code null} if no such tool exists.
     */
    ITool getToolEntry(String id);

    /**
     * Retrieve a bookmark by its ID.
     * 
     * @param id The tool ID
     * @return The Tool object with the specified ID, or {@code null} if no such bookmark exists.
     */
    Bookmark getBookmark(String id);

    /**
     * Add a new tool entry to the toolbox. Only non-bookmark types are supported
     * to be added as generic entries.
     * 
     * @param tool The ToolEntry to add. Must not be {@code null}.
     * @return The added ToolEntry
     * @throws NoSuchToolException If the ToolEntry does not represent a tool in the catalog
     * @throws DuplicateException If the ToolEntry is already present in the toolbox
     * @throws InvalidToolException If the ToolEntry is not valid
     */
    ToolEntry addToolEntry(ToolEntry tool) throws NoSuchToolException, DuplicateException, InvalidToolException;

    /**
     * Add a new bookmark to the toolbox.
     * 
     * @param tool The Bookmark to add. Must not be {@code null}.
     * @return The added Bookmark
     * @throws DuplicateException If the Bookmark is already present in the toolbox
     * @throws InvalidToolException If the Bookmark is not valid
     */
    Bookmark addBookmark(Bookmark tool) throws DuplicateException, InvalidToolException;

    /**
     * Delete a tool entry from the toolbox. If the tool entry is a Bookmark
     * in the toolbox, that Bookmark entry is also removed.
     * 
     * @param id The tool ID
     * @return The deleted ToolEntry object with the specified ID
     */
    ITool deleteToolEntry(String id);

    /**
     * Delete a bookmark from the toolbox. The tool entry is also removed.
     * 
     * @param id The bookmark ID
     * @return The deleted Bookmark object with the specified ID
     */
    Bookmark deleteBookmark(String id);

    /**
     * Reset the entire toolbox to the default configuration. Note that this
     * will cause persistence to occur. <b>Use with caution.</b>
     */
    void reset();

}