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
 * Specifies the statistics provided by a JMS session.
 */
public interface JMSSessionStats extends Stats {

    /*
     * Returns a list of JMSProducerStats that provide statistics about the message
     * producers associated with the referencing JMS session statistics.
     */
    public JMSProducerStats[] getProducers();

    /*
     * Returns a list of JMSConsumerStats that provide statistics about the message
     * consumers associated with the referencing JMS session statistics.
     */
    public JMSConsumerStats[] getConsumers();

    /*
     * Returns the number of messages exchanged.
     */
    public CountStatistic getMessageCount();

    /*
     * Returns the number of pending messages.
     */
    public CountStatistic getPendingMessageCount();

    /*
     * Returns the number of expired messages.
     */
    public CountStatistic getExpiredMessageCount();

    /*
     * Returns the number of expired messages.
     */
    public TimeStatistic getMessageWaitTime();

    /*
     * Returns the number of durable subscriptions.
     */
    public CountStatistic getDurableSubscriptionCount();

}
