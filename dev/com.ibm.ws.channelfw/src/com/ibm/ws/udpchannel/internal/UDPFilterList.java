/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import com.ibm.ws.tcpchannel.internal.FilterList;

/**
 * Contains the only the protected members of the FilterList object that I
 * couldn't call in my AccessLists object.
 */
public class UDPFilterList extends FilterList {
    @Override
    protected void setActive(boolean value) {
        super.setActive(value);
    }

    @Override
    protected boolean getActive() {
        return super.getActive();
    }

    @Override
    protected void buildData(String[] data, boolean validateOnly) {
        super.buildData(data, validateOnly);
    }

}
