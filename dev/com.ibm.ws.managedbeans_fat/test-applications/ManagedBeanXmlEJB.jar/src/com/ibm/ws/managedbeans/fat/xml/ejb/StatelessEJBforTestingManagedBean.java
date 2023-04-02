/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.xml.ejb;

/**
 * Basic Stateless bean local interface for testing Managed Beans.
 **/
public interface StatelessEJBforTestingManagedBean {
    public static final String APP = "ManagedBeanXmlApp";
    public static final String MOD = "ManagedBeanXmlEJB";

    /**
     * Verifies that a ManagedBean is properly injected per the
     * configuration of the bean, and that the ManagedBean may
     * or may not be looked up, depending on the configuration.
     **/
    public void verifyManagedBeanInjectionAndLookup();
}
