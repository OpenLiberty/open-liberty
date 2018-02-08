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

public class EntryAdapterFloatUnableBefore implements EntryAdapter<Float> {
    @Override
    public Float adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactEntry artifactEntry,
        Entry entry) throws UnableToAdaptException {

        String name = entry.getName();
        if ( name.equals("ab") || name.equals("bb") ) {
            // let all through to the next adapter.
            return null;
        } else if ( name.equals("aa") ) {
            return new Float(10);
        } else {
            return new Float(1);
        }
    }
}
