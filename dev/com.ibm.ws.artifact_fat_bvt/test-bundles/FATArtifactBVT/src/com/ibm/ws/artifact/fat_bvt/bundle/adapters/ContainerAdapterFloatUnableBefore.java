/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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

public class ContainerAdapterFloatUnableBefore implements ContainerAdapter<Float> {
    @Override
    public Float adapt(
        Container rootContainer,
        OverlayContainer rootOverlayContainer,
        ArtifactContainer artifactContainer,
        Container adaptedContainer) throws UnableToAdaptException {

        String adaptedContainerName = adaptedContainer.getName();
        if ( adaptedContainerName.equals("ab") ||
             adaptedContainerName.equals("bb") ) {
            // let all through to the next adapter.
            return null;
        } else if ( adaptedContainerName.equals("aa") ) {
            return new Float(10);
        } else {
            return new Float(1);
        }
    }
}
