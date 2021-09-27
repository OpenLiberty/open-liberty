/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.ejb;

public interface TimeoutFailureLocal {

    public static final long MAX_TIMER_WAIT = 3 * 60 * 1000;
    public final static long DURATION = 1000;

    public final static int RESULTS_NOT_RETRIED = -1;
    public final static int RESULTS_RETRIED_AS_EXPECTED = 0;
    public final static int RESULTS_BMT_TRAN_NOT_ROLLEDBACK = 1;
    public final static int RESULTS_TEST_ERROR = 2;

    public void prepRollbackCMTTimeout();

    public void prepThrowExceptionCMTTimeout();

    public void prepThrowExceptionInTxBMTTimeout();

    public int getResults();

    public void reset();
}
