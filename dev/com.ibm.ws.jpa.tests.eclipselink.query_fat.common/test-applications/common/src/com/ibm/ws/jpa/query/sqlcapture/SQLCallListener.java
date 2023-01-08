/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.query.sqlcapture;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SQLCallListener {
    private final static ArrayList<String> sqlCallList = new ArrayList<String>();

    public final static void recordSQLCall(String sql) {
        synchronized (sqlCallList) {
            sqlCallList.add(sql);
            System.out.println("SQLListener: Recorded SQL Call \"" + sql + "\".");
        }
    }

    public final static void clearCalls() {
        synchronized (sqlCallList) {
            sqlCallList.clear();
        }
    }

    public final static List<String> getAndClearCallList() {
        synchronized (sqlCallList) {
            final ArrayList<String> retList = new ArrayList<String>(sqlCallList);
            sqlCallList.clear();
            return retList;
        }
    }

    public final static List<String> peekCallList() {
        synchronized (sqlCallList) {
            final ArrayList<String> retList = new ArrayList<String>(sqlCallList);
            return retList;
        }
    }
}
