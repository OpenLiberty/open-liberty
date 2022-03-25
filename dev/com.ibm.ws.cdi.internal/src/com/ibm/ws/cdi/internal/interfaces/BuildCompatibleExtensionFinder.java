/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.interfaces;

import java.util.Set;

/**
 * Finds BuildCompatibleExtensions within an archive
 */
public interface BuildCompatibleExtensionFinder {

    /**
     * Finds the class names listed within the jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension service provider file within the archive
     *
     * @param archive the archive to search
     * @return the set of class names found
     */
    Set<String> findBceClassNames(CDIArchive archive);

}
