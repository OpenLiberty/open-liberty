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

import com.ibm.ws.javaee.dd.common.ResourceGroup;

public abstract class ResourceGroupComparator<T extends ResourceGroup> extends ResourceBaseGroupComparator<T> {

    private String trim(String s) {
        return s != null ? s.trim() : null;
    }

    @Override
    public boolean compare(T o1, T o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        if (o1.getLookupName() == null) {
            return o2.getLookupName() == null;
        } else {
            return o1.getLookupName().trim().equals(trim(o2.getLookupName()));
        }
    }

}
