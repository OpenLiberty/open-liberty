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
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import com.ibm.ws.javaee.dd.common.ResourceEnvRef;

/**
 *
 */
public class ResourceEnvRefComparator extends ResourceGroupComparator<ResourceEnvRef> {

    @Override
    public boolean compare(ResourceEnvRef o1, ResourceEnvRef o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        if (o1.getTypeName() == null) {
            if (o2.getTypeName() != null)
                return false;
        } else if (!o1.getTypeName().equals(o2.getTypeName())) {
            return false;
        }
        return compareDescriptions(o1.getDescriptions(), o2.getDescriptions());
    }

}
