/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5.containers;

/**
 * Interface for database containers that implement kerberos authentication
 */
public interface KerberosAuthContainer {
    /**
     * Get the Kerberos principle (user + realm) for this database
     *
     * @return principal
     */
    public String getKerberosPrinciple();

    /**
     * Get the Kerberos username for this database
     *
     * @return principal
     */
    public String getKerberosUsername();

    /**
     * Get the Kerberos password for the Kerberos user (principal) for this database
     *
     * @return password
     */
    public String getKerberosPassword();
}
