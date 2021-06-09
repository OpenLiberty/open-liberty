/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
public class SQLListener {
    private final static ArrayList<String> sqlList = new ArrayList<String>();

    public final static void recordSQL(String sql) {
        synchronized (sqlList) {
            sqlList.add(sql);
            System.out.println("SQLListener: Recorded SQL \"" + sql + "\".");
        }
    }

    public final static void clear() {
        synchronized (sqlList) {
            sqlList.clear();
        }
    }

    public final static List<String> getAndClear() {
        synchronized (sqlList) {
            final ArrayList<String> retList = new ArrayList<String>(sqlList);
            sqlList.clear();
            return retList;
        }
    }

    public final static List<String> peek() {
        synchronized (sqlList) {
            final ArrayList<String> retList = new ArrayList<String>(sqlList);
            return retList;
        }
    }
}
