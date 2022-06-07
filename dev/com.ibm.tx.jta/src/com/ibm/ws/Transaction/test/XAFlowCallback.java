package com.ibm.ws.Transaction.test;
/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

public interface XAFlowCallback
{
    // Flow type definitions
    public final static int FORGET   = 0;
    public final static int PREPARE  = 1;
    public final static int COMMIT   = 2;
    public final static int ROLLBACK = 3;

    //Before flag definitions
    public final static int FORGET_NORMAL         = 10;

    public final static int PREPARE_NORMAL        = 20;
    public final static int PREPARE_1PC_OPT       = 21;

    public final static int COMMIT_2PC            = 30;
    public final static int COMMIT_1PC_OPT        = 31;

    public final static int ROLLBACK_NORMAL       = 40;
    public final static int ROLLBACK_DUE_TO_ERROR = 41;

    //After flag definitions
    public final static int AFTER_SUCCESS         = 50;
    public final static int AFTER_FAIL            = 51;

        
    /**
     * Called before the current resource is flowed the XA signal 
     * defined in flowType.
     * 
     * @param flowType The current signal type:<br>
     *                 
     *                 <UL>
     *                 <LI>FORGET</LI>
     *                 <LI>PREPARE</LI>
     *                 <LI>COMMIT</LI>
     *                 <LI>ROLLBACK</LI>
     *                 </UL>
     * @param flag     A call specific flag that provides more detail about the type of call about
     *                 to be made to the resource:
     *                 
     *                 <UL>
     *                 <LI>FORGET_NORMAL</LI>
     *                 <LI>PREPARE_NORMAL</LI>
     *                 <LI>PREPARE_1PC_OPT</LI>
     *                 <LI>COMMIT_2PC</LI>
     *                 <LI>COMMIT_1PC_OPT</LI>
     *                 <LI>ROLLBACK_NORMAL</LI>
     *                 <LI>ROLLBACK_DUE_TO_ERROR</LI>
     *                 </UL>
     * 
     * @return True - Current resource is flowed the current signal type<br>
     *         False - Current resource is skipped
     */
    public boolean beforeXAFlow(int flowType, int flag);

    /**
     * Called before the current resource has been flowed the XA signal 
     * defined in flowType.
     * 
     * @param flowType The current signal type:<br>
     *                 
     *                 <UL>
     *                 <LI>FORGET</LI>
     *                 <LI>PREPARE</LI>
     *                 <LI>COMMIT</LI>
     *                 <LI>ROLLBACK</LI>
     *                 </UL>
     * @param flag     Flag specifying whether or not the call to the resource succeded:
     *                 
     *                 <UL>
     *                 <LI>AFTER_SUCCESS</LI>
     *                 <LI>AFTER_FAIL</LI>
     *                 </UL>
     * 
     * @return Not currently used.
     */
    public boolean afterXAFlow(int flowType, int flag);
}
