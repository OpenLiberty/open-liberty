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
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class ContainerAdapterFloatUnableAfter implements ContainerAdapter<Float> {
    @Override
    public Float adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer artifactContainer,
        Container adaptedContainer) throws UnableToAdaptException {

        String adaptedContainerName = adaptedContainer.getName();
        if ( adaptedContainerName.equals("aa") ||
             adaptedContainerName.equals("ab") ||
             adaptedContainerName.equals("bb") ) {
            // Should be handled by a prior adapter: Answer Float(999) as an error value.
            return new Float(999);
        } else {
            return new Float(0);
        }
    }
}
