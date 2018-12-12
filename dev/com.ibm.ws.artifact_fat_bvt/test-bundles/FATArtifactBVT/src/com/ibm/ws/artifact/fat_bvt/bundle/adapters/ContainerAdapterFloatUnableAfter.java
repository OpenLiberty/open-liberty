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
