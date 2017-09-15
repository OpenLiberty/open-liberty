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
 * Specifies the base interface for the statistics provided by a JMS message producer or
 * a JMS message consumer.
 */
public interface JMSEndpointStats extends Stats {

    /*
     * Returns the number of messages sent or received.
     */
    public CountStatistic getMessageCount();

    /*
     * Returns the number of pending messages.
     */
    public CountStatistic getPendingMessageCount();

    /*
     * Returns the number of messages that expired before delivery.
     */
    public CountStatistic getExpiredMessageCount();

    /*
     * Returns the time spent by a message before being delivered.
     */
    public TimeStatistic getMessageWaitTime();

}
