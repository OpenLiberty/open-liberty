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
package com.ibm.ws.cdi.internal.interfaces;

import java.util.Set;

import org.jboss.weld.ejb.spi.EjbDescriptor;

import com.ibm.ws.cdi.CDIException;

/**
 *
 */
public interface EndPointsInfo {

    Set<ManagedBeanDescriptor<?>> getManagedBeanDescriptors();

    Set<EjbDescriptor<?>> getEJBDescriptors();

    Set<Class<?>> getNonCDIInterceptors() throws CDIException;
}
