/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.statistics;

/**
 * This interface represents access to all statistics counters for a component.
 * Named statistics can be retrieved using the methods in this interface, and
 * helper functions exist for performing useful calculations in statistics
 * monitoring.
 * 
 * Each statistic has two measurements:
 * 
 * <table>
 * <tr>
 * <td>Count</td>
 * <td>A simple cumulative count of number of transactions that have been
 * performed since initialization. This will be a long integer.</td>
 * </tr>
 * <tr>
 * <td>ElapsedTime</td>
 * <td>The sum of all elapsed time spent processing each transaction. This will
 * be a BigInteger and measured in milliseconds.</td>
 * </tr>
 * </table>
 * 
 * Each time a transaction statistic is recorded by the component itself, the
 * count increments by one and the elapsed time will be incremented by the
 * amount of time that transaction took to execute.
 * 
 * To make meaningful use of the statistics you need to plot calculations from
 * the statistics counters polled over time periods (samples).
 * 
 * For example consider a sample period of 5 minutes (i.e. 300 seconds) At start
 * time T1 for a particular statistic counter, Count(T1) is taken, and
 * ElapsedTime(T1) is also taken. After 5 minutes, at time T2, two averages are
 * calculated for the sample period as follows:
 * 
 * <table>
 * <tr>
 * <td>Txns/sec for the sample period</td>
 * <td>(Count(T2)-Count(T1)) / 300</td>
 * </tr>
 * <tr>
 * <td>Average Turnaround Time during the sample period</td>
 * <td>(ElapsedTime(T2)-ElapsedTime(T1)) / (Count(T2)-Count(T1))</td>
 * </tr>
 * </table>
 * 
 * These calculated values should be considered plot points on a graph which
 * continues to have plot points added each 5 minutes (or whatever the poll
 * period is). For example at time T3, which is 300 seconds later than T2:
 * 
 * <ul>
 * <li>Txns/sec = (Count(T3)-Count(T2)) / 300</li>
 * <li>Average Turnaround Time = (ElapsedTime(T3)-ElapsedTime(T2) /
 * (Count(T3)-Count(T2))</li>
 * </ul>
 * 
 * Helper functions exist in this interface to calulate the txns/sec and average
 * turnaround time for two snapshots of the same counter.
 * 
 * Choosing your sample period is critical to getting meaningful charts, and
 * will be dependent on your system load. If the load is high, the sample period
 * can be shortened. If the load is low, the sample period can be lengthened. If
 * the sample period is too long, the graph will tend to flatline and spikes in
 * use will go unnoticed. If the sample period is too short, the graph will tend
 * to be very jagged and not look meaningful. More detailed information on this
 * modeling pattern is available in this article:
 * 
 * http://www.ibm.com/developerworks/tivoli/library/t-websealstat/
 * 
 * @see OAuthStatistic
 * @see OAuthStatisticNames
 * 
 */
public interface OAuthStatistics {

    /**
     * Returns the named counter snapshot at the present time, or null if
     * statName not recognized.
     * 
     * @param statName
     *            - name of a statistics counter supported by the component
     * @return the named counter snapshot at the present time, or null if
     *         statName not recognized
     */
    public OAuthStatistic getCounter(String statName);

    /**
     * Helper function to plot txn/sec as described above. Note:
     * t2.getTimestamp() - t1.getTimestamp() represents the sample period.
     * 
     * @param t1
     *            statistics counter from beginning of sample period
     * @param t2
     *            statistics counter from end of sample period
     * @return (Count(t2)-Count(t1)) / (timestamp(t2)-timestamp(t1) in seconds)
     */
    public double getTransactionsPerSecond(OAuthStatistic t1, OAuthStatistic t2);

    /**
     * Helper function to calculate average turnaround time as described above.
     * Note: t2.getTimestamp() - t1.getTimestamp() represents the sample period.
     * 
     * @param t1
     *            statistics counter from beginning of sample period
     * @param t2
     *            statistics counter from end of sample period
     * @return (ElapsedTime(t2)-ElapsedTime(t1) in milliseconds) /
     *         (Count(t2)-Count(t1))
     */
    public double getAverageTurnaroundTimeMilliseconds(OAuthStatistic t1,
            OAuthStatistic t2);

}
