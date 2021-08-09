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
 * The JDBCStats type specifies the statistics provided by a JDBC resource.
 */
public interface JDBCStats extends Stats {

    /*
     * Returns a list of JDBCConnectionStats that provide statistics about the nonpooled
     * connections associated with the referencing JDBC resource statistics.
     */
    public JDBCConnectionStats[] getConnections();

    /*
     * Returns a list of JDBCConnectionPoolStats that provide statistics about the
     * connection pools associated with the referencing JDBC resource statistics.
     */
    public JDBCConnectionPoolStats[] getConnectionPools();

}
