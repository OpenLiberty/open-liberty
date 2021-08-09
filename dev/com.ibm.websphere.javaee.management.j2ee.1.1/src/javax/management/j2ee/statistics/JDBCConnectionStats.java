/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.management.j2ee.statistics;


/**
 * Specifies the statistics provided by a JDBC connection.
 */
public interface JDBCConnectionStats extends Stats {

    /*
     * Returns the name of the managed object that identifies the JDBC data source
     * for this connection.
     */
    public String getJdbcDataSource();

    /*
     * Returns the time spent waiting for a connection to be available.
     */
    public TimeStatistic getWaitTime();

    /*
     * Returns the time spent using a connection.
     */
    public TimeStatistic getUseTime();

}
