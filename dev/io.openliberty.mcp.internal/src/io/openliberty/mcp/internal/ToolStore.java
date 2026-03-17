/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.mcp.tools.ToolManager.ToolInfo;

/**
 * Thread-safe storage and retrieval of tools.
 * <p>
 * This class separates that parts of {@link ToolRegistry} that need synchronization to be thread-safe.
 */
public class ToolStore {

    private static final TraceComponent tc = Tr.register(ToolStore.class);

    /**
     * Must synchronize before write access. Must call {@link #refreshToolList()} after modifying.
     */
    private ConcurrentHashMap<String, ToolMetadata> tools = new ConcurrentHashMap<>();

    /**
     * Read only. Updated by {@link #refreshToolList()}.
     */
    private volatile List<ToolMetadata> toolList = Collections.emptyList();

    /**
     * Updates toolInfoList from tools
     * <p>
     * Must be called after updating {@link #tools} to keep {@link #toolList} in sync
     * <p>
     * Must hold lock when calling this.
     */
    private void refreshToolList() {
        assert Thread.holdsLock(this);
        toolList = tools.values()
                        .stream()
                        .sorted()
                        .toList();
    }

    /**
     * Add a tool to the store
     *
     * @param tool the tool to add
     * @throws IllegalArgumentException if a tool with the same name already exists in the store
     */
    public void addTool(ToolMetadata tool) {
        synchronized (this) {
            ToolMetadata oldValue = tools.putIfAbsent(tool.name(), tool);
            if (oldValue != null) {
                String message = Tr.formatMessage(tc, "CWMCM0026E.duplicate.tool.name", tool.name());
                throw new IllegalArgumentException(message);
            } else {
                refreshToolList();
            }
        }
    }

    /**
     * Remove a tool from the store
     *
     * @param name the name of the tool
     * @return the tool that was removed, or {@code null} if no tool with that name existed in the store
     */
    public ToolMetadata removeTool(String name) {
        synchronized (this) {
            ToolMetadata removedTool = tools.remove(name);
            if (removedTool != null) {
                refreshToolList();
            }
            return removedTool;
        }
    }

    /**
     * Get the tool with the given name
     *
     * @param name the tool name
     * @return the tool with the given name, or {@code null} if no tool exists with that name
     */
    public ToolMetadata getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get an ordered, unmodifiable list of tools as {@link ToolMetadata} objects
     *
     * @return the ordered list of tools
     */
    public List<ToolMetadata> getAllToolMetadata() {
        return toolList;
    }

    /**
     * Get an ordered, unmodifiable list of tools as {@link ToolInfo} objects
     * <p>
     * This is a convenience method for interfaces that require a collection or stream of ToolInfo rather than ToolMetadata
     *
     * @return the ordered list of tools
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<ToolInfo> getAllToolInfos() {
        // This cast from List<ToolMetadata> -> List<ToolInfo> is safe
        // because toolList is not modifiable.
        return (List) toolList;
    }

}
