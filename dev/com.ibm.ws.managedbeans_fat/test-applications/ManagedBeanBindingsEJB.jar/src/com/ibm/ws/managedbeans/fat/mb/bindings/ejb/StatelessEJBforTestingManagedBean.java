/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.bindings.ejb;

/**
 * Basic Stateless bean local interface for testing Managed Beans.
 **/
public interface StatelessEJBforTestingManagedBean {
    public static final String APP = "ManagedBeanBindingsApp";
    public static final String MOD = "ManagedBeanBindingsEJB";

    /**
     * Verifies that a ManagedBean is properly injected per the
     * configuration of the bean, and that the ManagedBean may
     * or may not be looked up, depending on the configuration.
     **/
    public void verifyManagedBeanInjectionAndLookup();
}
