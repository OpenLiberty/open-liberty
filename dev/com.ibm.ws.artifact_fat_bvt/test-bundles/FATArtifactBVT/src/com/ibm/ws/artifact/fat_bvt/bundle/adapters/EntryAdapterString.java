/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2017
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.fat_bvt.bundle.adapters;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class EntryAdapterString implements EntryAdapter<String> {

    @Override
    public String adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactEntry artifactEntry,
        Entry entry) throws UnableToAdaptException {

        String rootContainerID = ContainerUtils.getContainerID(rootContainer);

        String entryID = ContainerUtils.getEntryID(entry);

        String entryPath = entry.getPath();

        Entry alternateEntry = rootContainer.getEntry(entryPath);
        if ( alternateEntry == null ) {
            return "FAIL: Root container [ " + rootContainerID + " ] does not container entry [ " + entryPath + " ] corresponding to [ " + entryID + " ]";
        }

        String alternatePath = alternateEntry.getPath();
        if ( !alternatePath.equals(entryPath) ) {
            return "FAIL: Root container [ " + rootContainerID + " ] entry [ " + entryPath + " ] has unexpected path [ " + alternatePath + " ]";
        }

        return "Verified entry [ " + entryID + " ]";
    }
}
