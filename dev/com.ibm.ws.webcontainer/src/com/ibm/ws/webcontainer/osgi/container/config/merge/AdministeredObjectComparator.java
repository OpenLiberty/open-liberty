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
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import com.ibm.ws.javaee.dd.common.AdministeredObject;

public class AdministeredObjectComparator extends AbstractBaseComparator<AdministeredObject> {

    @Override
    public boolean compare(AdministeredObject o1, AdministeredObject o2) {
        if (o1.getName() == null) {
            if (o2.getName() != null)
                return false;
        } else if (!o1.getName().equals(o2.getName())) {
            return false;
        }
        if (o1.getInterfaceNameValue() == null) {
            if (o2.getInterfaceNameValue() != null)
                return false;
        } else if (!o1.getInterfaceNameValue().equals(o2.getInterfaceNameValue())) {
            return false;
        }
 
        if (o1.getClassNameValue() == null) {
            if (o2.getClassNameValue() != null)
                return false;
        } else if (!o1.getClassNameValue().equals(o2.getClassNameValue())) {
            return false;
        }

        if (o1.getName() == null) {
            if (o2.getName() != null)
                return false;
        } else if (!o1.getName().equals(o2.getName())) {
            return false;
        }
            
        if (!compareProperties(o1.getProperties(), o2.getProperties())) {
            return false;
        }
        if (!compareDescriptions(o1.getDescriptions(), o2.getDescriptions())) {
            return false;
        }
        if (o1.getResourceAdapter() == null) {
            if (o2.getResourceAdapter() != null)
                return false;
        } else if (!o1.getResourceAdapter().equals(o2.getResourceAdapter())) {
            return false;
        }
        return true;
    }

}
