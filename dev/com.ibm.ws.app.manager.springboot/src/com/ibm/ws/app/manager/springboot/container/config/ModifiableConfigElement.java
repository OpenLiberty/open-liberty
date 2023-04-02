/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * Represents a config element that allows modification.
 */
public interface ModifiableConfigElement {

    /**
     * Modifies the element.
     * 
     * @param config The server configuration
     * @throws Exception when an error occurs 
     */
    public void modify(ServerConfiguration config) throws Exception;
}
