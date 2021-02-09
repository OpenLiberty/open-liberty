/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension;

/**
 * This is a *marker* interface for Weld Runtime extension. All runtime extensions need to register a service
 * under this interface. This bundle will find all of the services and then get hold of the bundle classloader and
 * pass onto Weld.
 * <p>
 * This interface has been deprecated. CDIExtensionMetadata should be used instead.
 */
@Deprecated
public interface WebSphereCDIExtension {}
