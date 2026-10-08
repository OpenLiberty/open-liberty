/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json;

/**
 * This class contains the Json properties and values.
 *
 * @since 2.1
 */
public final class JsonConfig {

    /**
     * Configuration property to define the strategy for handling duplicate keys.
     *
     * See {@link KeyStrategy}
     */
    public static final String KEY_STRATEGY = "jakarta.json.JsonConfig.keyStrategy" ;

    /**
     * It avoids new instances of this class.
     */
    private JsonConfig() {}
    
    /**
     * Contains the different values allowed for {@link #KEY_STRATEGY}.
     *
     * See {@link #KEY_STRATEGY}
     */
    public static enum KeyStrategy {
        /**
         * Configuration value that will take the value of the first match.
         */
        FIRST,
        /**
         * Configuration value that will take the value of the last match.
         */
        LAST,
        /**
         * Configuration value that will throw {@link JsonException} when duplicate key is found.
         */
        NONE;
    }
}
