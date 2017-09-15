/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.specification;

import java.util.List;
import java.util.Set;

public interface ClassSource_Specification_Direct_WAR extends ClassSource_Specification_Direct {
    // When set, use this as the classes location; otherwise, default to
    // a location relative to the immediate location.

    String getWARClassesPath();

    void setWARClassesPath(String warClassesPath);

    // Three cases:
    //
    // No WAR library path, no WAR library paths:
    //   Select from the location relative to the immediate location.
    // Set WAR library path:
    //   Select from the specified path.
    // Set WAR library paths:
    //   Use the specified paths.

    String getWARLibraryPath();

    void setWARLibraryPath(String warLibraryPath);

    boolean getUseWARLibraryJarPaths();

    void setUseWARLibraryJarPaths(boolean useWARLibraryJarPaths);

    List<String> getWARLibraryJarPaths();

    void addWARLibraryJarPath(String warLibraryJarPath);

    void addWARLibraryJarPaths(List<String> warLibraryJarPaths);

    // Optional selection of a subset of the WAR libraries.
    // This is necessary for distinguishing the non-metadata complete fragments.
    // Metadata-complete fragments are not seed locations.

    Set<String> getWARIncludedJarPaths();

    void addWARIncludedJarPath(String includedJarPath);

    void addWARIncludedJarPaths(Set<String> includedJarPaths);
}
