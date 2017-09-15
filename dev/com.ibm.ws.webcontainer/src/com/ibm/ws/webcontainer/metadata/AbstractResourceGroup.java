/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.metadata;

import com.ibm.ws.javaee.dd.common.ResourceGroup;

/**
 *
 */
public abstract class AbstractResourceGroup extends AbstractResourceBaseGroup implements ResourceGroup {

    private String lookupName;

    public AbstractResourceGroup(ResourceGroup resourceGroup) {
        super(resourceGroup);
        this.lookupName = resourceGroup.getLookupName();
        if (lookupName != null) {
            lookupName = lookupName.trim();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getLookupName() {
        return lookupName;
    }

}
