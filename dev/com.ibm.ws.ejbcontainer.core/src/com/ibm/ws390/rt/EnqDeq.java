/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.rt;

public class EnqDeq
{
    /**
     * EnqDeq is a static utility class used to obtain and release
     * OS/390 ENQs. This is accomplished by using the native services
     * in the ORB.
     */
    static final int SUCCESS = 0;
    static final int FAILED = 4;

    /**
     * SSBeanEnq is a deprecated method, but left in for scaffolding. @P1C
     * It will assume a DIRECT IOR and an ENQ scope of SYSTEM is required.
     * Also, assumes routing by SR Affinity is desired.
     */
    public static int SSBeanEnq(byte[] pk)
    {
        // Assume DIRECT IOR & Enq Scope of SYSTEM                           @P1A
        return EnqDeq.SSBeanEnq(pk, true, true); // @MD16624C
    };

    /**
     * SSBeanDeq is a deprecated method, but left in for scaffolding. @P1C
     * It will assume a DIRECT IOR and an ENQ scope of SYSTEM is required.
     * Also, assumes routing by SR Affinity is desired.
     */
    public static int SSBeanDeq(byte[] pk)
    {
        // Assume DIRECT IOR & Enq Scope of SYSTEM                           @P1A
        return EnqDeq.SSBeanDeq(pk, true, true); // @MD16624C
    };

    /*
     * SSBeanEnq is called by the container to obtain a SYSTEMs
     * ENQ called SYSZBBO.<binary primary key uuid> which is used by
     * the control region to find the server region which currently
     * contains the stateful session instance identified by its primary key.
     * The PK is used to create the rname of the ENQ, thus the CR can find
     * the SR given the primary key of the target stateful session object.
     * 
     * @param pk A byte array containing the stateful session bean primary key UUID
     * 
     * @param srAffinity TRUE (1) if object has affinity to the SR (implies an ENQ
     * of a SYSTEM scope). False (0) if object has no affinity to a particular SR
     * (implies an ENQ of SYSTEMS scope).
     * 
     * @param WLMTempAff TRUE (1) if WLM is to be told about object Temporal Affinity
     * 
     * @return SUCCESS (0) if ENQ was successfully obtained, FAILED (4) if a problem
     * was encountered obtaining the ENQ
     * 
     * @exception none
     */
    public native static int SSBeanEnq(byte[] pk, boolean srAffinity, boolean WLMTempAff); // @P1A,@MD16624C

    /**
     * SSBeanDeq is called by the container to release the ENQ
     * obtained by SSBeanEnq above.
     * 
     * @param pk A byte array containing the stateful session bean primary key UUID
     * @param srAffinity TRUE (1) if object has affinity to the SR (implies an ENQ
     *            of a SYSTEM scope). False (0) if object has no affinity to a particular SR
     *            (implies an ENQ of SYSTEMS scope).
     * @param WLMTempAff TRUE (1) if WLM is to be told about object Temporal Affinity
     * 
     * @return SUCCESS (0) if ENQ was successfully released, FAILED (4) if a problem
     *         was encountered releasing the ENQ
     * 
     * @exception none
     */
    public native static int SSBeanDeq(byte[] pk, boolean srAffinity, boolean WLMTempAff); // @P1A,@MD16624C
}
