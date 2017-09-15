/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.management.j2ee.internal;

import javax.management.ObjectName;

import com.ibm.websphere.management.j2ee.EntityBeanMBean;
import com.ibm.websphere.management.j2ee.J2EEManagedObject;

public class EntityBean extends J2EEManagedObject implements EntityBeanMBean {
    public EntityBean(ObjectName objectName) {
        super(objectName);
    }
}
