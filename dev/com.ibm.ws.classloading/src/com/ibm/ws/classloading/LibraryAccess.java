/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading;

import java.util.Collection;

import com.ibm.wsspi.library.Library;

/**
 * The LibraryAccess service allows packages to be configured with a library to grant access to the
 * packages from OSGi bundles contained in the liberty kernel region.
 */
public interface LibraryAccess {
    /**
     * Indicates what will have access to the library packages that are configured.
     */
    public enum PackageVisibility {
        /**
         * Packages will be accessible from OSGi applications.
         */
        OSGI_APPS,
        /**
         * Packages will be accessible from the liberty feature bundles contained in the kernel region.
         */
        LIBERTY_FEATURES
    }

    /**
     * Configures a collection of packages for a library that will become available for
     * import from OSGi bundles.
     * 
     * @param library the library where the packages will be loaded from.
     * @param packageNames the package export statements. Uses the OSGi Export-Package syntax
     * @param visibility indicates what bundles will have visibility to the exported packages.
     */
    void setPackages(Library library, Collection<String> packageNames, PackageVisibility visibility);

}
