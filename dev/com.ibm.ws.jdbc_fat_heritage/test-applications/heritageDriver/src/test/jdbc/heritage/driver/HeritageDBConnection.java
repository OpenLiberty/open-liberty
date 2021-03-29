/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.driver;

import java.util.Set;

/**
 * Interface for invoking vendor-specific API on the test Heritage JDBC Driver.
 */
public interface HeritageDBConnection {
    /**
     * XA start flag for loosely coupled transaction branches.
     */
    public static int LOOSELY_COUPLED_TRANSACTION_BRANCHES = 0x1000;

    /**
     * Obtains the list of valid client info keys.
     *
     * @return the list of valid client info keys.
     */
    Set<String> getClientInfoKeys();

    /**
     * Sets the list of valid keys for client info.
     *
     * @param value the new list of valid keys for client info. If empty, the defaults are used.
     */
    void setClientInfoKeys(String... keys);
}