/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.tracker;

import java.util.NoSuchElementException;

import com.ibm.websphere.csi.J2EEName;

/**
 * Interface for version-dependent capability, available as an OSGi service.
 */
public interface ModuleTracker {
    /**
     * Obtains the name of the first component (sorted alphabetically)
     * in the module. This is only available for Enterprise Bean modules.
     *
     * @param moduleName the JEE name of the module.
     * @return name of first component in the module.
     * @throws NoSuchElementException if the module does not contain any components.
     */
    String firstComponentName(J2EEName moduleName);
}