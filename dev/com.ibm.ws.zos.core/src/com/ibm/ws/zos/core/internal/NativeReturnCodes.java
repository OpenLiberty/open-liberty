/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.internal;

/**
 * This class defines return codes which are shared with the native code. Any native part
 * in the z/OS core component should add its return codes here so that they are all mapped
 * in one place in the Java code.
 */
public class NativeReturnCodes {
    /** Server registration with the angel was successful */
    public static final int ANGEL_REGISTER_OK = 0;

    /** Server registration with the angel failed because there was no master BGVT control block */
    public static final int ANGEL_REGISTER_NO_BGVT = 1;

    /** Server registration with the angel failed because there was no CGOO control block */
    public static final int ANGEL_REGISTER_NO_CGOO = 2;

    /** Server registration with the angel failed because there was no system LX in the CGOO control block */
    public static final int ANGEL_REGISTER_NO_LX = 3;

    /** Server registration with the angel failed because the angel is inactive */
    public static final int ANGEL_REGISTER_ANGEL_INACTIVE = 4;

    /** Server registration with the angel failed because the angel name could not be found. */
    public static final int ANGEL_REGISTER_ANGEL_NAME_NOT_EXIST = 5;

    /* Return codes 256 <= x < 4096 are issued in the fixed shim module */
    /** Server registration with the angel failed because the ARMV hung off of the SGOO is inactive */
    public static final int ANGEL_REGISTER_FSM_INACT_ARMV = 256;

    /** Server registration with the angel failed with an unknown error in the dynamic replaceable module */
    public static final int ANGEL_REGISTER_FSM_ERROR = 257;

    /** Server registration with the angel failed because there was no SGOO control block */
    public static final int ANGEL_REGISTER_FSM_NO_SGOO = 258;

    /** Server registration with the angel failed because there was no storage to create an ARR control block */
    public static final int ANGEL_REGISTER_FSM_NO_ARR_STORAGE = 259;

    /** Server registration with the angel failed because the server is already registered with the angel */
    public static final int ANGEL_REGISTER_FSM_ALREADY_REG = 260;

    /**
     * Server registration with the angel failed because there are one or more tasks which are still processing work on behalf of a previous registration of this server with the
     * angel
     */
    public static final int ANGEL_REGISTER_FSM_ACTIVE_TGOO = 261;

    /* Return codes 4096 <= x are issued in the dynamic replaceable module */
    /** Server registration with the angel failed with an unknown error in the dynamic replaceable module */
    public static final int ANGEL_REGISTER_DRM_FAIL = 4096;

    /** Server registration with the angel failed because the PGOO for this server could not be marked as active */
    public static final int ANGEL_REGISTER_DRM_MARK_PGOO = 4097;

    /** Server registration with the angel failed because a local copy of the ASVT control block could not be allocated */
    public static final int ANGEL_REGISTER_DRM_ALLOCATE_ASVT = 4098;

    /** Server registration with the angel failed because a TGOO control block could not be allocated */
    public static final int ANGEL_REGISTER_DRM_ALLOCATE_TGOO = 4099;

    /** Server registration with the angel failed because a PGOO control block could not be allocated */
    public static final int ANGEL_REGISTER_DRM_ALLOCATE_PGOO = 4100;

    /** Server registration with the angel failed because there was no storage left in the below-the-bar heap */
    public static final int ANGEL_REGISTER_DRM_ALLOC31_FAIL = 4101;

    /** Server registration with the angel failed because a RESMGR could not be created to monitor the server address space */
    public static final int ANGEL_REGISTER_DRM_RESMGR_FAIL = 4102;

    /** Server registration with the angel failed because the server was not authorized to connect to the angel */
    public static final int ANGEL_REGISTER_DRM_NOT_AUTHORIZED = 4103;

    /** Server registration with the angel failed because the server was not authorized to load the BBGZSAFM module. */
    public static final int ANGEL_REGISTER_DRM_NOT_AUTHORIZED_BBGZSAFM = 4104;

    /** Server registration with the angel detected module SAFM is not APF authorized */
    public static final int ANGEL_REGISTER_DRM_SAFM_NOT_APF_AUTHORIZED = 4107;

    /** Server registration with the angel failed for an unspecified reason. */
    public static final int ANGEL_REGISTER_UNDEFINED = 65535;

    /** Invoke processing successfuly drove the target method */
    public static final int ANGEL_INVOKE_OK = 0;

    /** Invoke processing failed because the master BGVT control block does not exist. */
    public static final int ANGEL_INVOKE_NO_BGVT = 1;

    /** Inovke processing failed because the CGOO control block does not exist */
    public static final int ANGEL_INVOKE_NO_CGOO = 2;

    /** Invoke processing failed because the LX for the angel process does not exist */
    public static final int ANGEL_INVOKE_NO_LX = 3;

    /* Return codes 256 <= x < 4096 are issued in the fixed shim module */
    /** Invoke processing failed because the SGOO control block could not be found */
    public static final int ANGEL_INVOKE_FSM_NO_SGOO = 256;

    /** Invoke processing failed because the PGOO control block could not be found */
    public static final int ANGEL_INVOKE_FSM_NO_PGOO = 257;

    /* Return codes 4096 <= x are issued in the dynamic replaceable module */
    /** Invoke processing failed for an unspecified reason in the dynamic replaceable module */
    public static final int ANGEL_INVOKE_DRM_FAIL = 4096;

    /** Invoke processing failed because the in-use task count in the PGOO could not be incremented */
    public static final int ANGEL_INVOKE_DRM_INC_PGOO = 4097;

    /** Invoke processing failed because the TGOO for this task could not be located */
    public static final int ANGEL_INVOKE_DRM_NO_TGOO = 4098;

    /** Invoke processing failed because the requested function was not found */
    public static final int ANGEL_INVOKE_DRM_UNREG_FUNC = 4099;

    /** Invoke processing failed because the server is not authorized to call the requested function */
    public static final int ANGEL_INVOKE_DRM_UNAUTH_FUNC = 4100;

    /** Invoke processing failed for an unspecified reason */
    public static final int ANGEL_INVOKE_UNDEFINED = 65535;

    /** Server deregistration from the angel was successful */
    public static final int ANGEL_DEREGISTER_OK = 0;

    /** Server deregistration from the angel was successful but there are still tasks invoking services. Deregistration will complete when the tasks return. */
    public static final int ANGEL_DEREGISTER_OK_PENDING = 4;

    /** Server deregistration from the angel failed because the master BGVT control block could not be found. */
    public static final int ANGEL_DEREGISTER_NO_BGVT = 8;

    /** Server deregistration from the angel failed because the CGOO control block could not be found. */
    public static final int ANGEL_DEREGISTER_NO_CGOO = 9;

    /** Server deregistration from the angel failed because the LX was not set */
    public static final int ANGEL_DEREGISTER_NO_LX = 10;

    /* Return codes 256 <= x < 4096 are issued in the fixed shim module */
    /** Server deregistration from the angel failed because the SGOO control block could not be found */
    public static final int ANGEL_DEREGISTER_FSM_NO_SGOO = 256;

    /** Server deregistration from the angel failed because the PGOO control block could not be found */
    public static final int ANGEL_DEREGISTER_FSM_NO_PGOO = 257;

    /* Return codes 4096 <= x are issued in the dynamic replaceable module */
    /** Server deregistration from the angel failed for an unspecified reason in the dynamically replaceable module */
    public static final int ANGEL_DEREGISTER_DRM_FAIL = 4096;

    /** Server deregistration from the angel failed because the server was not registered with the angel. */
    public static final int ANGEL_DEREGISTER_DRM_ALR_DEREG = 4097;

    /** Server deregistration from the angel failed for an unspecified reason. */
    public static final int ANGEL_DEREGISTER_UNDEFINED = 65535;

}
