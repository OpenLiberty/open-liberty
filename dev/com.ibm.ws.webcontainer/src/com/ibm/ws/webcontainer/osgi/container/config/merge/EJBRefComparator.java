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

import com.ibm.ws.javaee.dd.common.EJBRef;

public class EJBRefComparator extends ResourceGroupComparator<EJBRef> {

    @Override
    public boolean compare(EJBRef o1, EJBRef o2) {
        if (!super.compare(o1, o2)) {
            return false;
        }
        if (o1.getHome() == null) {
            if (o2.getHome() != null)
                return false;
        } else if (!o1.getHome().equals(o2.getHome())) {
            return false;
        }
        if (o1.getInterface() == null) {
            if (o2.getInterface() != null)
                return false;
        } else if (!o1.getInterface().equals(o2.getInterface())) {
            return false;
        }
        if (o1.getKindValue() != o2.getKindValue()) {
            return false;
        }
        if (o1.getLink() == null) {
            if (o2.getLink() != null)
                return false;
        } else if (!o1.getLink().equals(o2.getLink())) {
            return false;
        }
        if (o1.getTypeValue() != o2.getTypeValue()) {
            return false;
        }
        return compareDescriptions(o1.getDescriptions(), o2.getDescriptions());
    }

}
