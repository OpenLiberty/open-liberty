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
package com.ibm.ws.cdi.ejb.impl;

import org.jboss.weld.ejb.spi.EjbDescriptor;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;

public interface WebSphereEjbDescriptor<T> extends EjbDescriptor<T> {

    /**
     * @return The full J2EEName of the EJB
     */
    public J2EEName getEjbJ2EEName();

    /**
     * @return the EJBReferenceFactory used to create EJB References
     */
    public EJBReferenceFactory getReferenceFactory();

}
