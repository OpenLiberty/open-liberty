/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal.services;

import java.util.Collection;
import java.util.List;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;

/**
 * Handles reading the merge include/exclude configuration properties and indicating whether a particular module should be included or excluded.
 */
public interface ModuleSelectionConfig {

    /**
     * Provide a human friendly summary of the current configuration.
     */
    @Override
    public String toString();

    /**
     * Whether the legacy "first module only" mode should be used.
     * <p>
     * As this requires special handling, if this method returns {@code true}, the other methods on this object should not be called.
     *
     * @return {@code true} if only the first module found should be processed for OpenAPI annotations, {@code false} otherwise.
     */
    public boolean useFirstModuleOnly();

    /**
     * Whether the given module should be used to create the OpenAPI document, based on the config
     *
     * @param module the module to check
     * @return {@code true} if the module should be used, {@code false} otherwise
     */
    public boolean isIncluded(ModuleInfo module);

    /**
     * Given a complete list of all application modules deployed, return a list of entries from the include configuration which didn't match any of the deployed modules.
     *
     * @param moduleInfos the deployed module infos
     * @return the list of include configuration entries which was unused
     */
    public List<String> findIncludesNotMatchingAnything(Collection<? extends ModuleInfo> moduleInfos);

}
