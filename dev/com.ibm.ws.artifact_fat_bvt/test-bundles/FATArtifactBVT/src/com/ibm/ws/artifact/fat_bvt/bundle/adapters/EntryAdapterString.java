/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
