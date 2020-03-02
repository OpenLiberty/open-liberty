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

/**
 * Represents a value looked up from an {@link ExtendedConfigSource}. The value can either be present or not. If it is present, it can either be a String or {@code null}.
 */
public interface ConfigString {

    /**
     * Constant for a ConfigString representing a value which is not present
     */
    ConfigString MISSING = new ConfigString() {

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String getValue() {
            return null;
        }

    };

    /**
     * Returns whether the requested value is present in the ConfigSource
     *
     * @return whether the requested value is present
     */
    boolean isPresent();

    /**
     * Returns the value from the ConfigSource. The result of this method has no meaning if {@link #isPresent()} returns {@code false}.
     *
     * @return the value from the ConfigSource, which may be {@code null}
     */
    String getValue();

    /**
     * Creates a ConfigString for a value which is present in the ConfigSource
     *
     * @param value the value from the ConfigSource
     * @return the new ConfigString
     */
    static ConfigString of(String value) {
        return new ConfigString() {

            @Override
            public boolean isPresent() {
                return true;
            }

            @Override
            public String getValue() {
                return value;
            }
        };
    }
}