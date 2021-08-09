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

import com.ibm.ws.javaee.dd.common.ResourceBaseGroup;

public abstract class ResourceBaseGroupComparator<T extends ResourceBaseGroup> extends AbstractBaseComparator<T> {

    @Override
    public boolean compare(T o1, T o2) {
        if (o1.getName() == null) {
            if (o2.getName() != null)
                return false;
        } else if (!o1.getName().equals(o2.getName())) {
            return false;
        }
        if (o1.getMappedName() == null) {
            if (o2.getMappedName() != null)
                return false;
        } else if (!o1.getMappedName().equals(o2.getMappedName())) {
            return false;
        }
        return true;
    }
}
