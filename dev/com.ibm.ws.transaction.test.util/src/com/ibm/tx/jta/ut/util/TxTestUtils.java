/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;

import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.uow.UOWScope;
import com.ibm.ws.uow.UOWScopeCallback;
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

    private static final int DEFAULT_REQUEST_TIMEOUT = 300000;

    public static void setTimeouts(Map<String, Object> requestContext) {
        setTimeouts(requestContext, DEFAULT_REQUEST_TIMEOUT);
    }

    public static void setTimeouts(Map<String, Object> requestContext, int timeout) {
        requestContext.put("com.sun.xml.ws.connect.timeout", timeout);
        requestContext.put("com.sun.xml.ws.request.timeout", timeout);
        requestContext.put("javax.xml.ws.client.connectionTimeout", timeout);
        requestContext.put("javax.xml.ws.client.receiveTimeout", timeout);
    }

    public static UOWScopeCallback registerUOWScopeCallback() {
    	UOWScopeCallback cb = new UOWScopeCallbackImpl();
    	UserTransactionImpl.instance().registerCallback(cb);
    	return cb;
    }

    public static void unregisterUOWScopeCallback(UOWScopeCallback cb) {
    	UserTransactionImpl.instance().unregisterCallback(cb);
    }

    public static boolean allCallbacksCalled(UOWScopeCallback cb) {
    	return ((UOWScopeCallbackImpl)cb).allCallbacksCalled();
    }
    
    public static boolean onlyBeginCallbacksCalled(UOWScopeCallback cb) {
    	return ((UOWScopeCallbackImpl)cb).onlyBeginCallbacksCalled();
    }

    private static class UOWScopeCallbackImpl implements UOWScopeCallback {

		// Constants defined in dev/com.ibm.tx.jta/src/com/ibm/ws/Transaction/UOWCallback.java#L27
		// static public final int PRE_BEGIN  = 0;
        // static public final int POST_BEGIN = 1;
        // static public final int PRE_END    = 2;
        // static public final int POST_END   = 3;
    	private Set<Integer> _contextChanges = new HashSet<Integer>();

    	public boolean allCallbacksCalled() {
			return _contextChanges.contains(PRE_BEGIN) && _contextChanges.contains(POST_BEGIN) && _contextChanges.contains(PRE_END) && _contextChanges.contains(POST_END);
		}
    	
    	public boolean onlyBeginCallbacksCalled() {
			return _contextChanges.contains(PRE_BEGIN) && _contextChanges.contains(POST_BEGIN) && !_contextChanges.contains(PRE_END) && !_contextChanges.contains(POST_END);
		}

        @Override
        public void contextChange(int changeType, UOWScope uowScope) throws IllegalStateException {
        	
            System.out.println("Change Type: " + changeType + ", UOWScope: " + uowScope);
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                System.out.println(ste + "\n");
            }
			
            _contextChanges.add(changeType);
        }
    }
}
