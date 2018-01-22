/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.SortedSet;

import com.ibm.ws.repository.resolver.internal.resource.FeatureResource;
import com.ibm.ws.repository.resolver.internal.resource.IFixResource;
import com.ibm.ws.repository.resolver.internal.resource.ResourceImpl;

/**
 * Compares the type of ResourceImpls to each other, {@link FeatureResource}s are more than {@link IFixResource}s. If either the resources being compared is not a feature or a fix
 * or they are the same type then they are said to be equal (i.e. {@link #compare(ResourceImpl, ResourceImpl)} returns 0). Therefore this comparator is not consisent with equals
 * and cannot be used in places where this is a requirement (for instance in {@link SortedSet}). The implementation is mutually comparable.
 */
public class FixFeatureComparator implements Comparator<ResourceImpl>, Serializable {

    private static final long serialVersionUID = -6145477571622512622L;

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(ResourceImpl o1, ResourceImpl o2) {
        if (o1 instanceof IFixResource) {
            if (o2 instanceof FeatureResource) {
                return -1;
            }
        } else if (o1 instanceof FeatureResource && o2 instanceof IFixResource) {
            return 1;
        }
        return 0;
    }

}
