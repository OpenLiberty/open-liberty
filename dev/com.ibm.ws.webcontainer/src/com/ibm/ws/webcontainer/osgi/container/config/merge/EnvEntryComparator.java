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

import com.ibm.ws.javaee.dd.common.EnvEntry;

public class EnvEntryComparator extends ResourceGroupComparator<EnvEntry> {

    @Override
    public boolean compare(EnvEntry o1, EnvEntry o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        if (o1.getValue() == null) {
            if (o2.getValue() != null)
                return false;
        } else if (!o1.getValue().equals(o2.getValue())) {
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
