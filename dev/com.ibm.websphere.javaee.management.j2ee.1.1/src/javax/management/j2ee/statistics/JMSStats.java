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
 * The JMSStats interface specifies the statistics provided by a JMS resource.
 */
public interface JMSStats extends Stats {

    /*
     * Returns a list of JMSConnectionStats that provide statistics about the
     * connections associated with the referencing JMS resource.
     */
    public JMSConnectionStats[] getConnections();

}
