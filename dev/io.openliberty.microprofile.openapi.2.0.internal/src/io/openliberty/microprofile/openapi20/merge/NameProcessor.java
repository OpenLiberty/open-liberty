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

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Tracks the names in use and the renames that occur during the model merge process
 * <p>
 * An instance of this class is held by the {@link MergeProcessor}, which creates an instance of {@link DocumentNameProcessor} for each document which is to be merged.
 */
public class NameProcessor {

    private static final TraceComponent tc = Tr.register(NameProcessor.class);

    /**
     * The unique names and their values which are in use for models processed so far, grouped by the type of name
     */
    private Map<NameType, Map<String, Object>> namesInUse;

    public NameProcessor() {
        this.namesInUse = new HashMap<>();
    }

    /**
     * Create a processor which stores and creates new unique names for model elements within a particular document.
     * 
     * @return the new document name processor
     */
    public DocumentNameProcessor createDocumentNameProcessor() {
        return new DocumentNameProcessor();
    }

    /**
     * Creates and stores new unique names for model elements.
     * <p>
     * Certain names must be unique within an OpenAPI document. When merging documents, these names may have to be changed so that they don't clash.
     * <p>
     * This class handles detecting clashes, creating new names as required and looking up the new names of renamed elements.
     * <p>
     * A new instance of this class should be created using {@link NameProcessor#createDocumentNameProcessor()} for every input document in a merge operation.
     */
    public class DocumentNameProcessor {
        private Map<NameType, Map<String, String>> renames = new HashMap<>();
        private boolean hasRenames = false;

        private DocumentNameProcessor() {}

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
            return NameProcessor.this.namesInUse.computeIfAbsent(nameType, (k) -> new HashMap<>());
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
                    Tr.event(tc, "Renamed " + nameType + " " + oldName + " -> " + newName);
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
}