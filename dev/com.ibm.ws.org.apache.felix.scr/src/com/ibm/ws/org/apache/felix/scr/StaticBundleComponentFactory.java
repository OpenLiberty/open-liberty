/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.org.apache.felix.scr;

/**
 * Bundles which which to avoid reflective management of their
 * SCR components implement and register an implementation of this
 * interface via a bundle header. Implementations of this are expected
 * to be able to construct and inject fields for all components in their
 * bundle. 
 */
public interface StaticBundleComponentFactory {

    public StaticComponentManager createStaticComponentManager(String componentName);
}