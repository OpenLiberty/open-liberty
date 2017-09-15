/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.sib.ra;

/**
 * Constants for use with <code>SICoreConnectionFactorySelector</code> along
 * with a <code>FactoryType</code> of <code>RA_CONNECTION</code.
 */
public interface SibRaConstants {

    /**
     * Key for property to indicate who will provide the credentials for
     * authentication with the messaging engine.
     */
    public static String CREDENTIAL_PROVIDER = "CREDENTIAL_PROVIDER";

    /**
     * Value for property indicating that the caller will provide the
     * credentials for authentication with the messaging engine.
     */
    public static String CREDENTIAL_PROVIDER_CALLER = "CREDENTIAL_PROVIDER_CALLER";

    /**
     * Value for property indicating that the container will provide the
     * credentials for authentication with the message engine using the given
     * mapping module.
     */
    public static String CREDENTIAL_PROVIDER_CONTAINER = "CREDENTIAL_PROVIDER_CONTAINER";

    /**
     * Key for property containing the name of the mapping module to use with
     * container authentication.
     */
    public static String CONTAINER_MAPPING_MODULE = "CONTAINER_MAPPING_MODULE";

    /**
     * Key for property containing the name of the alias to use with the default
     * principal mapping with container provided credentials.
     */
    public static String CONTAINER_AUTHENTICATION_ALIAS = "CONTAINER_AUTHENTICATION_ALIAS";

    /**
     * Key for property containing the name of the default principal mapping
     * alias to use during XA recovery.
     */
    public static String XA_RECOVERY_ALIAS = "XA_RECOVERY_ALIAS";

    /**
     * Key for property indicating whether or not, within a global transaction,
     * multiple connections may be enlisted via a single <code>XAResource</code>.
     */
    public static String CONNECTION_SHARING = "CONNECTION_SHARING";

    /**
     * Value for property indicating that connections should not be shared.
     */
    public static String CONNECTION_SHARING_DISABLED = "CONNECTION_SHARING_DISABLED";

    /**
     * Value for property indicating that connections may be shared.
     */
    public static String CONNECTION_SHARING_ENABLED = "CONNECTION_SHARING_ENABLED";

    /**
     * Key for property indicating whether or not a warning should be logged
     * when the connection manager is accessed in the absence of a transaction
     * context.
     */
    public static String MISSING_TRANSACTION_CONTEXT = "MISSING_TRANSACTION_CONTEXT";

    /**
     * Value for property indicating that missing transaction contexts should be
     * logged (the default behaviour).
     */
    public static String MISSING_TRANSACTION_CONTEXT_LOG = "MISSING_TRANSACTION_CONTEXT_LOG";

    /**
     * Value for property indicating that missing transaction contexts should
     * not be logged.
     */
    public static String MISSING_TRANSACTION_CONTEXT_IGNORE = "MISSING_TRANSACTION_CONTEXT_IGNORE";

}