/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.interfaces;

/**
 *
 */
public interface SourcedValue<T> {

    /**
     * Get the actual value
     *
     * @return the value
     */
    public T getValue();

    /**
     * Get the ID of the source that provided the value
     *
     * @return the originating source ID
     */
    public String getSource();

}
