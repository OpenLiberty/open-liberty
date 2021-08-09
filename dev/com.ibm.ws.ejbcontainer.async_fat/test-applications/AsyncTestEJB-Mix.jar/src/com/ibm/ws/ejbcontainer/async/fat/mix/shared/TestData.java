/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.mix.shared;

public class TestData {
    /**
     * Static variables for thread the method is executing on for comparison to
     * caller thread
     **/
    private static long bsb_syncMethThreadId;
    private static long bsb_syncMeth2ThreadId;
    private static long bsb_asyncMethThreadId;

    public static long getBsb_syncMethThreadId() {
        return bsb_syncMethThreadId;
    }

    public static long getBsb_syncMeth2ThreadId() {
        return bsb_syncMeth2ThreadId;
    }

    public static long getBsb_asyncMethThreadId() {
        return bsb_asyncMethThreadId;
    }

    public static void setBsb_syncMethThreadId(long tid) {
        bsb_syncMethThreadId = tid;
    }

    public static void setBsb_syncMeth2ThreadId(long tid) {
        bsb_syncMeth2ThreadId = tid;
    }

    public static void setBsb_asyncMethThreadId(long tid) {
        bsb_asyncMethThreadId = tid;
    }
}