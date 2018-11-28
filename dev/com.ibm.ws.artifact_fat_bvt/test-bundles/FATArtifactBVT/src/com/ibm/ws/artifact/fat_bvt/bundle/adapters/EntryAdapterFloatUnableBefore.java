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
