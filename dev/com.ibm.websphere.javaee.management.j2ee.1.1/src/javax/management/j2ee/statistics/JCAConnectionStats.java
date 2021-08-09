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
 * The JCAConnectionStats interface specifies the statistics provided by a JCA
 * connection.
 */
public interface JCAConnectionStats extends Stats {

    /*
     * Returns the JCAConnectionFactory string of the managed object
     * that identifies the connector’s connection factory for this connection.
     */
    public String getConnectionFactory();

    /*
     * Returns the JCAManagedConnectionFactory string of the
     * managed object that identifies the connector’s managed connection factory for
     * this connection.
     */
    public String getManagedConnectionFactory();

    /*
     * Returns time spent waiting for a connection to be available.
     * 
     * public TimeStatistic getWaitTime();
     * 
     * /*
     * Returns the time spent using a connection.
     */
    public TimeStatistic getUseTime();

    /*
     * Returns the time spent waiting for a connection to be available.
     */
    public TimeStatistic getWaitTime();

}
