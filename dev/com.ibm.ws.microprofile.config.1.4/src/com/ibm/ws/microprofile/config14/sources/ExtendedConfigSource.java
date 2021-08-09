/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.sources;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Extends the {@code getValue} method so that a null value can be differentiated from non-existence.
 * <p>
 * Implementing this interface allows getValue to be called without a secondary call to {@link #getPropertyNames()}
 */
public interface ExtendedConfigSource extends ConfigSource {

    /**
     * Returns the value for the specified property in this configuration source, wrapped in a {@link ConfigString}.
     * <p>
     * If no value for the specified property is stored in this configuration source, a {@code ConfigString} is returned where the {@link ConfigString#isPresent() isPresent} method
     * returns
     * {@code false}.
     *
     * @param propertyName the propertyName
     * @return a {@code ConfigString} containing the value
     */
    public ConfigString getConfigString(String propertyName);

}
