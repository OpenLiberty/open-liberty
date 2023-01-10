/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.library.spi;

/**
 * A library configured in server.xml which requires SPI package visibility.
 */
public interface SpiLibrary {

    /**
     * Return a unique class loader, for the specified ownerId and configured library, that
     * can see SPI packages in addition to the library binaries and API types. There should
     * be at most one of these in existence at any time for each <library,ownerId>.
     * 
     * @param ownerId a non-empty string used to create a purposeful class loader identity.
     */
    ClassLoader getSpiClassLoader(String ownerId);

}
