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
import com.ibm.ws.ui.internal.v1.pojo.FeatureTool;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * The catalog is the collection of all available tools for use by UI users.
 * The information about the tools stored in the catalog is system wide.
 */
public interface ICatalog extends SelfValidatingPOJO {

    /**
     * Metadata field (Long): the last time the catalog contents were modified.
     */
    String METADATA_LAST_MODIFIED = "lastModified";

    /**
     * Metadata field (Boolean): represents the catalog is the default catalog
     * and has never been modified.
     */
    String METADATA_IS_DEFAULT = "isDefault";

    /**
     * Retrieve the catalog metadata. The returned Map is a read-only
     * snapshot of the metadata at the time it was asked for.
     * 
     * <p>See the METADATA_* fields defined in this interface.</p>
     * 
     * @return A Map of all of the catalog metadata
     */
    Map<String, Object> get_metadata();

    /**
     * Retrieve all of the feature tools in the catalog.
     * The returned List is a read-only snapshot of the set of tools at the time it was asked for.
     * 
     * @return The List of all FeatureTool objects in the catalog, {@code null} is not returned.
     */
    List<FeatureTool> getFeatureTools();

    /**
     * Retrieve all of the bookmarks in the catalog.
     * The returned List is a read-only snapshot of the set of bookmarks at the time it was asked for.
     * 
     * @return The List of all Bookmark objects in the catalog, {@code null} is not returned.
     */
    List<Bookmark> getBookmarks();

    /**
     * Retrieve a tool by its ID. The tool could be either feature tool or a bookmark.
     * 
     * @param id The tool ID
     * @return The Tool object with the specified ID, or {@code null} if no such tool exists.
     */
    ITool getTool(String id);

    /**
     * Retrieve a feature tool by its ID.
     * 
     * @param id The feature tool ID
     * @return The FeatureTool object with the specified ID, or {@code null} if no such tool exists.
     */
    FeatureTool getFeatureTool(String id);

    /**
     * Retrieve a bookmark by its ID.
     * 
     * @param id The bookmark ID
     * @return The Bookmark object with the specified ID, or {@code null} if no such bookmark exists.
     */
    Bookmark getBookmark(String id);

    /**
     * Add a new bookmark to the catalog.
     * 
     * @param bookmark The Bookmark to add
     * @return Returns the added Bookmark
     * @throws DuplicateException If the Bookmark is already present in the catalog
     * @throws InvalidToolException If the Bookmark is not valid
     */
    Bookmark addBookmark(Bookmark bookmark) throws DuplicateException, InvalidToolException;

    /**
     * Delete a bookmark from the catalog.
     * 
     * @param id The bookmark ID
     * @return The deleted Bookmark object with the specified ID, or {@code null} if the bookmark is not defined.
     */
    Bookmark deleteBookmark(String id);

    /**
     * Reset the entire catalog to the default configuration. Note that this
     * will cause persistence to occur. <b>Use with caution.</b>
     */
    void reset();

}