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

import com.ibm.ws.javaee.dd.common.PersistenceContextRef;

/**
 *
 */
public class PersistenceContextRefComparator extends ResourceBaseGroupComparator<PersistenceContextRef> {

    @Override
    public boolean compare(PersistenceContextRef o1, PersistenceContextRef o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        if (o1.getPersistenceUnitName() == null) {
            if (o2.getPersistenceUnitName() != null)
                return false;
        } else if (!o1.getPersistenceUnitName().equals(o2.getPersistenceUnitName())) {
            return false;
        }
        if (o1.getTypeValue() != o2.getTypeValue()) {
            return false;
        }
        if (!compareProperties(o1.getProperties(), o2.getProperties())) {
            return false;
        }
        return compareDescriptions(o1.getDescriptions(), o2.getDescriptions());
    }

}
