/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
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
