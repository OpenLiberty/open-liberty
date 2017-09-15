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
 * Specifies the statistics provided by a JMS connection.
 */
public interface JMSConnectionStats extends Stats {

    /*
     * Returns a list of JMSSessionStats that provide statistics about the sessions
     * associated with the referencing JMSConnectionStats.
     */
    public JMSSessionStats[] getSessions();

    /*
     * Returns the transactional state of this JMS connection. If true, indicates that
     * this JMS connection is transactional.
     */
    public boolean isTransactional();
}
