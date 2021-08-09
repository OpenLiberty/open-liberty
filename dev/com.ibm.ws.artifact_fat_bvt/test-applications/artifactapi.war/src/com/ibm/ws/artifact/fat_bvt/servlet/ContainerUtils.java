/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class ContainerUtils {
    public static String getEntryID(Entry entry) throws UnableToAdaptException {
        Container enclosingContainer =
            entry.getEnclosingContainer();
        String enclosingContainerId =
            ContainerUtils.getContainerID(enclosingContainer); // throws UnableToAdaptException

        String entryName = entry.getName();

        if ( enclosingContainer.isRoot() ) {
            // "/" + name: The enclosing ID has a trailing "/": Don't add another slash.
            return enclosingContainerId + entryName;
        } else {
            // "/parent/child" + "/" + name: The enclosing ID does not have a trailing "/".  Add one.
            return enclosingContainerId + "/" + entryName;
        }
    }

    public static String getEntryIDNoGetRoot(Entry entry) throws UnableToAdaptException {
        Container enclosingContainer =
            entry.getEnclosingContainer();
        String enclosingContainerId =
            ContainerUtils.getContainerIDNoGetRoot(enclosingContainer); // throws UnableToAdaptException

        String entryName = entry.getName();

        if ( enclosingContainer.isRoot() ) {
            // "/" + name: The enclosing ID has a trailing "/": Don't add another slash.
            return enclosingContainerId + entryName;
        } else {
            // "/parent/child" + "/" + name: The enclosing ID does not have a trailing "/".  Add one.
            return enclosingContainerId + "/" + entryName;
        }
    }

    public static String getContainerID(Container entryContainer)
        throws UnableToAdaptException {

        String containerID = entryContainer.getPath();
        Container rootContainer = entryContainer.getRoot();

        Entry entryInParent;
        while ( (entryInParent = rootContainer.adapt(Entry.class)) != null) {
            containerID = entryInParent.getPath() + "#" + containerID;
            rootContainer = entryInParent.getRoot();
        }

        return containerID;
    }

    public static String getContainerIDNoGetRoot(Container entryContainer)
        throws UnableToAdaptException {

        String containerID = entryContainer.getPath();
        Container rootContainer = getRootContainer(entryContainer);

        Entry entryInParent;
        while ( (entryInParent = rootContainer.adapt(Entry.class)) != null) {
            containerID = entryInParent.getPath() + "#" + containerID;
            rootContainer = getRootContainer( entryInParent.getEnclosingContainer() );
        }

        return containerID;
    }

    public static Container getRootContainer(Container container) {
        while ( !container.isRoot() ) {
            container = container.getEnclosingContainer();
        }
        return container;
    }
}
