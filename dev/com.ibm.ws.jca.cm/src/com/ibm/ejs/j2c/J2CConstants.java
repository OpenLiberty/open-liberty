/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.security.AccessController;

import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.ws.util.dopriv.GetThreadContextAccessorPrivileged;

/**
 * This interface defines constants used by j2c interfaces, implementation
 * classes, and client interfaces and implementation classes.
 * Interface name : J2CConstants
 *
 * Packaging : interface jar and client jar
 */
public interface J2CConstants {

    public static final String traceSpec = "WAS.j2c";

    public static final String messageFile = "com.ibm.ws.j2c.resources.J2CAMessages";

    /**
     * NLS file for messages that are unique to the liberty profile.
     */
    public static final String NLS_FILE = "com.ibm.ws.jca.cm.internal.resources.J2CAMessages";

    /**
     * Constant for application managed authentication.
     *
     */
    public static final int AUTHENTICATION_APPLICATION = 1; // value = APPLICATION

    /**
     * Constant for container managed authentication.
     *
     */
    public static final int AUTHENTICATION_CONTAINER = 0; // value = CONTAINER

    /**
     * Constant for shareable connections.
     *
     */
    public static final int CONNECTION_SHAREABLE = 0; // value = SHAREABLE

    /**
     * Constant for unshareable connections.
     *
     */
    public static final int CONNECTION_UNSHAREABLE = 1; // value = com.ibm.websphere.csi.ResRef.UNSHAREABLE

    /**
     * Used when throwing ConnectionWaitTimeoutException when the
     * connection pool is full.
     *
     */
    public final static String DMSID_MAX_CONNECTIONS_REACHED = "Max connections reached";

    public static final int LIFECYCLE_STATUS_REQ_FAILED = 0; // 0=Status request failed
    public static final int LIFECYCLE_STATUS_ACTIVE = 1; // 1=Active
    public static final int LIFECYCLE_STATUS_PAUSED = 2; // 2=Paused
    public static final int LIFECYCLE_STATUS_STOPPED = 3; // 3=Stopped
    //4=Paused-Mixed: some of the ConnectionFactories, DataSources, and
    //   ActivationSpecs are Pause while others are active; Valid for ResourceAdapterMBean only.
    public static final int LIFECYCLE_STATUS_MIXED = 4;
    public static final int HA_CAPABILITY_OFF = 0; // 0=HA disabled
    public static final int HA_CAPABILITY_ENDPOINT_SINGLETON = 1; // 1=HA is enabled for inbound only
    public static final int HA_CAPABILITY_RA_SINGLETON = 2; // 2=HA is enabled for inbound and outbound

    public static final int LIFECYCLE_STATUS_UNKNOWN = 99; //99=Failed

    public static final int INITIAL_SIZE = 500;

    public enum JCASpecVersion {
        JCA_VERSION_16,
        JCA_VERSION_15,
        JCA_VERSION_10
    }

    // New runtime utility, ThreadContextAccessor,
    // replaces Thread API methods getContextClassLoader()
    // and setConextClassLoader().
    //
    // Method ThreadContextAccessor.getThreadContextAccessor()
    // checks permissions required to get and set the context
    // class loader.  So, use the GetThreadContextAccessorPrivileged
    // utility exactly once to obtain the instance.  Use the
    // ThreadContextAccessor methods getContextClassLoader() and
    // setContextClassLoader() to get and set the contextClassLoader
    // field of the current thread w/o incurring the permission
    // checks that execute using the Thread API whenever Java2
    // security is enabled.
    @SuppressWarnings("unchecked")
    public static final ThreadContextAccessor TCA = (ThreadContextAccessor) AccessController.doPrivileged(new GetThreadContextAccessorPrivileged());

    /**
     * The value of com.ibm.wsspi.security.token.AttributeNameConstants.WSCREDENTIAL_CACHE_KEY.
     */
    static final String WSCREDENTIAL_CACHE_KEY = "com.ibm.wsspi.security.cred.cacheKey";

    /**
     * Names of XA recovery properties in the embeddable EJB container.
     * In J2C, we include these properties in ConnectorProperties.
     *
     */
    static final String XA_RECOVERY_PASSWORD = "xaRecoveryPassword",
                    XA_RECOVERY_USER = "xaRecoveryUser";

    public final static String POOL_ConnectionTimeout = "connectionTimeout";

    public final static String POOL_ReapTime = "reapTime";
    public final static String POOL_AgedTimeout = "agedTimeout";

    public final static String POOL_PurgePolicy = "purgePolicy";

    public final static String SECURITY_OptionC_authDataAlias = "OptionC_authDataAlias";

    public final static String XA_RECOVERY_AUTH_ALIAS = "XA_RECOVERY_AUTH_ALIAS";

    public final static String MAPPING_MODULE_mappingConfigAlias = "mappingConfigAlias";
    public final static String MAPPING_MODULE_authDataAlias = "authDataAlias";

}