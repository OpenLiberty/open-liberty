/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.classsource.specification;

import java.util.Collection;
import java.util.List;

public interface ClassSource_Specification_Direct_WAR extends ClassSource_Specification_Direct {

    // Details for the classes folder ...

    boolean getIgnoreClassesPath();
    void setIgnoreClassesPath(boolean ignoreClassesPath);

    String DEFAULT_CLASSES_PATH = "WEB-INF/classes/";
    String getDefaultClassesPath();

    String getClassesPath();
    void setClassesPath(String warClassesPath);

    // Details for the lib folder ...

    boolean getIgnoreLibPath();
    void setIgnoreLibPath(boolean ignoreLibPath);

    String DEFAULT_LIB_PATH = "WEB-INF/lib/";
    String getDefaultLibPath();

    String getLibPath();
    void setLibPath(String libPath);

    // Details for specific libraries ...
    
    List<String> getLibPaths();

    void addLibPath(String libPath);
    void addLibPath(String libPath, boolean isPartial, boolean isExcluded);

    void addLibPaths(List<String> libPaths);
    void addLibPaths(List<String> libPaths, boolean isPartial, boolean isExcluded);

    boolean isPartialPath(String libPath);
    void addPartialPath(String partialPath);
    void addPartialPaths(Collection<String> partialPaths);

    boolean isExcludedPath(String libPath);
    void addExcludedPath(String excludedPath);
    void addExcludedPaths(Collection<String> excludedPaths);
}
