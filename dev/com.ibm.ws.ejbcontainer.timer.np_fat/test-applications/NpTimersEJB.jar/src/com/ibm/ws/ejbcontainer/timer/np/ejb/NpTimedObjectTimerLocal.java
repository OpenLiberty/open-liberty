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

import java.util.concurrent.CountDownLatch;

public interface NpTimedObjectTimerLocal {

    /**
     * Create and cancel a timer. Both transaction of creation and cancellation are
     * committed. The timer should not be available after its cancellation.
     * This method is tested with Tx attributes BMT, required, and Not supported. <p>
     *
     * This is the preparation part.
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx01} <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prepCreateAndCancel(String txType);

    /**
     * Create and cancel a timer. Both transaction of creation and cancellation are committed. The
     * timer should not be available after its cancellation. <p>
     * This method is tested with Tx attributes BMT, required, and Not supported. <p>
     *
     * This is the result collection part.
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx01} <p>
     *
     */
    public void testCreateAndCancel();

    /**
     * Create a timer in a rolled back transaction. The timer should not be created <p>
     * This method is tested with Tx attributes BMT, Required
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx02} <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prepRollbackCreate(String txType);

    /**
     * Create a timer in a rolled back transaction. The timer should not be created <p>
     * This method is tested with Tx attributes BMT, Required
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx02} <p>
     *
     */
    public void testRollbackCreate();

    /**
     * Cancel a timer in a rolled back transaction. The timer should be still active. <p>
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx03} <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prep1RollbackCancel(String txType);

    /**
     * Cancel a timer in a rolled back transaction. The timer should be still active. <p>
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx03} <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prep2RollbackCancel(String txType);

    /**
     * Cancel a timer in a rolled back transaction. The timer should be still active. <p>
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx03} <p>
     *
     */
    public void testRollbackCancel();

    public void cancelAllTimers();

    /**
     * Test the behavior of the ejbTimeout method of a CMT Stateless session bean. The user
     * transaction in the method is committed. <p>
     *
     * Used by tests :
     * <ul>
     * <li> {@link TimerTxPersistenceTest#tmtx05}.
     * </ul> <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prepEjbTimeoutCommitTest(String txType);

    /**
     * Test the behavior of the ejbTimeout method of a CMT Stateless session bean. The user
     * transaction in the method is rollback. <p>
     *
     * Used by tests :
     * <ul>
     * <li> {@link TimerTxPersistenceTest#tmtx07}.
     * </ul> <p>
     *
     * @param txType Transaction Type - TX_BMT, TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prepEjbTimeoutRollbackTest(String txType);

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx06} <p>
     *
     */
    public void testTxExpirationBMT();

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx06} <p>
     *
     * @param txType Transaction Type - TX_REQUIRED, TX_REQUIRESNEW,
     *            or TX_NOTSUPPORTED
     */
    public void prepTxExpirationCMT(String txType);

    /**
     * If a timer is created in a transaction. The timer will not be expired if the transaction has
     * not been committed. The timer's expiration should be counted from the time the timer is
     * created instead of the timer the transaction is committed.
     *
     * Used by test : {@link TimerTxPersistenceTest#tmtx06} <p>
     *
     */
    public void testTxExpirationCMT();

    public void prepNoCommitTest();

    public void prepAccessAfterCancelTest(String info);

    public CountDownLatch cancelAccessAfterCancelTimer();

    public void waitForTimer(long maxWaitTime);
}
