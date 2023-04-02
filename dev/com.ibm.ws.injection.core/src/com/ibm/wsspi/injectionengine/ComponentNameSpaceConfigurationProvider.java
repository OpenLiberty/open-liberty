/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
