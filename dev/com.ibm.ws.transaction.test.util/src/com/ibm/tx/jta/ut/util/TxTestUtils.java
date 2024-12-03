/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.tx.jta.ut.util;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
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

    private static final String pattern = "dd/MM/uuuu, HH:mm.ss:SSS z";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(pattern);
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    public static final String traceTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    public static final String traceTime(FileTime timestamp) {
        return timestamp.toInstant().atZone(ZONE_ID).format(DATE_TIME_FORMATTER);
    }

    // This is an environment variable which should take the form 1,2,6
    // That would make connections 1,2 & 6 fail.
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

	public static void setTestResourcesFile() throws IOException {
		String recoveryId = System.getProperty("LOCAL_RECOVERY_ID");
		System.out.println("setTestResourcesFile: recoveryId prop="+recoveryId);
		if (recoveryId != null) {
			String resourcesDirPath = System.getenv("WLP_OUTPUT_DIR") + "/../shared/test-resources/" + recoveryId;
			File resourcesDir = new File(resourcesDirPath);
			// Create it if necessary
			if (!resourcesDir.exists()) {
				resourcesDir.mkdirs();
			}
			XAResourceImpl.setStateFile(new File(resourcesDir.getPath() + File.separator + "XAResources.dat"));
			System.out.println("setTestResourcesFile: "+XAResourceImpl.STATE_FILE);
		}
	}

	public static void setTimeouts(Map<String, Object> requestContext, int timeout) {
		requestContext.put("com.sun.xml.ws.connect.timeout", timeout);
		requestContext.put("com.sun.xml.ws.request.timeout", timeout);
		requestContext.put("javax.xml.ws.client.connectionTimeout", timeout);
		requestContext.put("javax.xml.ws.client.receiveTimeout", timeout);
	}
}


