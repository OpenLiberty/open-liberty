/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import com.ibm.wsspi.adaptable.module.Container;

public interface ClassSource_Specification_Container extends ClassSource_Specification {

    // Container names are only locally meaningful.
    // The name of a root container is always "/",
    // and the name of a child container is always local to it's
    // immediate parent.

    public static class ContainerData {
        public String name;
        public Container container;

        public ContainerData(String name, Container container) {
            super();

            this.name = name;
            this.container = container;
        }
    }

    // Not always needed; the WAR will disregard this location
    // if both the classes location and the WAR libraries are specified.

    String getImmediateContainerName();

    Container getImmediateContainer();

    void setImmediateContainerData(String name, Container immediateContainer);

    ContainerData getImmediateContainerData();

    void setImmediateContainerData(ContainerData immediateContainer);

    // Two cases:
    //
    // The library container is set:
    //   Select from the specified library container.
    // The library container not set:
    //   Use the specified library containers.

    String getApplicationLibraryContainerName();

    Container getApplicationLibraryContainer();

    void setApplicationLibraryContainerData(String name, Container applicationLibraryContainer);

    ContainerData getApplicationLibraryContainerData();

    void setApplicationLibraryContainerData(ContainerData applicationLibraryContainer);

    //

    List<ContainerData> getApplicationLibrariesContainerData();

    void addApplicationLibrariesContainerData(String name, Container applicationLibraryContainer);

    void addApplicationLibrariesContainerData(ContainerData applicationLibraryContainer);

    void addApplicationLibrariesContainerData(List<ContainerData> applicationLibraryContainers);

    // These are always specified as a list.  (Generally, the locations
    // are resolved relative to the container of the parent application.)

    List<ContainerData> getManifestContainerData();

    void addManifestContainerData(String name, Container manifestContainer);

    void addManifestContainerData(ContainerData manifestContainer);

    void addManifestContainerData(List<ContainerData> manifestContainers);
}
