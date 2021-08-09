/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

/**
 * Represents a config element that allows modification.
 *
 * To be used in conjunction with fat.modify tag on certain config elements.
 * This used to be used for database rotation but a new method of doing this is available
 * in the fattest.databases module.
 */
@Deprecated
public interface ModifiableConfigElement {

    /**
     * Modifies the element.
     */
    public void modify(ServerConfiguration config) throws Exception;
}
