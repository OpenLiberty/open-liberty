/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.interfaces;

import java.lang.reflect.Type;

/**
 * A value, the type of the value and the id of its source
 */
public interface SourcedValue {

    /**
     * Get the actual value
     *
     * @return the value
     */
    public Object getValue();

    /**
     * Get the type of the value
     *
     * @return
     */
    public Type getType();

    /**
     * Get the ID of the source that provided the value
     *
     * @return the originating source ID
     */
    public String getSource();

    @Override
    public String toString();

    /**
     * Get the property key
     *
     * @return the key
     */
    String getKey();
}
