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
 * The JCAStats interface specifies the statistics provided by a JCA resource.
 */
public interface JCAStats extends Stats {

    /*
     * Returns a list of JCAConnectionStats that provide statistics about the nonpooled
     * connections associated with the referencing JCA resource statistics.
     */
    public JCAConnectionStats[] getConnections();

    /*
     * Returns a a list of JCAConnectionPoolStats that provide statistics about the
     * connection pools associated with the referencing JCA resource statistics.
     */
    public JCAConnectionPoolStats[] getConnectionPools();

}
