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

import com.ibm.ws.container.service.config.ServletConfigurator.MergeComparator;

public class DefaultComparator implements MergeComparator<Object> {

    @Override
    public boolean compare(Object o1, Object o2) {
        if (o1 == null) {
            if (o2 != null) {
                return false;
            }
        } else if (!o1.equals(o2)) {
            return false;
        }
        return true;
    }

}
