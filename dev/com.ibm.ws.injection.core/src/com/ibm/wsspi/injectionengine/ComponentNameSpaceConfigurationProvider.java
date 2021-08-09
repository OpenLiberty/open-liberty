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
package com.ibm.wsspi.injectionengine;

/**
 * A provider for a component namespace configuration. This interface allows
 * the caller to delay creation of a ComponentNameSpaceConfiguration until it
 * is needed to resolve metadata.
 */
public interface ComponentNameSpaceConfigurationProvider
{
    ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration()
                    throws InjectionException;
}
