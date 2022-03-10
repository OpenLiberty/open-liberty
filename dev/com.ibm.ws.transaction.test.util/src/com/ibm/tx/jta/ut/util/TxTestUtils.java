/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.ut.util;

import java.net.ConnectException;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;

import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

/**
 *
 */
public class TxTestUtils {

    /**  */
    private static final long serialVersionUID = 1L;

	public static final String CONNECTION_MANAGER_FAILS = "CONNECTION_MANAGER_FAILS";

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

	private static int connectCount;

    public static String printStatus(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
                return "Status.STATUS_ACTIVE";
            case Status.STATUS_COMMITTED:
                return "Status.STATUS_COMMITTED";
            case Status.STATUS_COMMITTING:
                return "Status.STATUS_COMMITTING";
            case Status.STATUS_MARKED_ROLLBACK:
                return "Status.STATUS_MARKED_ROLLBACK";
            case Status.STATUS_NO_TRANSACTION:
                return "Status.STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARED:
                return "Status.STATUS_PREPARED";
            case Status.STATUS_PREPARING:
                return "Status.STATUS_PREPARING";
            case Status.STATUS_ROLLEDBACK:
                return "Status.STATUS_ROLLEDBACK";
            case Status.STATUS_ROLLING_BACK:
                return "Status.STATUS_ROLLING_BACK";
            default:
                return "Status.STATUS_UNKNOWN";
        }
    }

    public void tryUOWManagerLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final UOWManager uowm = (UOWManager) new InitialContext().lookup("java:comp/websphere/UOWManager");

        if (!(uowm instanceof UOWManager)) {
            throw new Exception("Lookup of java:comp/websphere/UOWManager failed");
        }

        final long localUOWId = uowm.getLocalUOWId();

        uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
            @Override
            public void run() throws Exception {
                if (localUOWId == uowm.getLocalUOWId()) {
                    throw new Exception("UOWAction not run under new UOW");
                }

                System.out.println("Expiration time: " + uowm.getUOWExpiration());
            }
        });
    }

	public static void scupperConnection() throws SQLException {

        String fails = System.getenv(CONNECTION_MANAGER_FAILS);
        System.out.println("SIMHADB: getDriverConnection: " + CONNECTION_MANAGER_FAILS + "=" + fails);

        HashSet<Integer> failSet = new HashSet<Integer>();
        if (fails != null) {
            StringTokenizer st = new StringTokenizer(fails, ",");
            while (st.hasMoreTokens()) {
                failSet.add(Integer.parseInt(st.nextToken()));
            }
        }

        connectCount++;
        System.out.println("SIMHADB: getDriverConnection: connectCount=" + connectCount);

        if (failSet.contains(connectCount)) {
            System.out.println("SIMHADB: getDriverConnection: scuppering now");
            throw new SQLNonTransientException(new ConnectException("Scuppering connection attempt number " + connectCount));
        }
	}
}
