/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.concurrent.persistent.mbean;

import javax.management.MXBean;

/**
 * The PersistentExecutorMBean is an MXBean.<br>
 * To get a PersistentExecutorMBean, query the MBean server with an object name specifying
 * PersistentExecutorMBean and the ID or display name of the PersistentExecutor<br>
 * For example: <br>
 * <code>
 * <pre> MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
 * ObjectName name = new ObjectName("WebSphere:type=PersistentExecutorMBean,id=defaultEJBPersistentTimerExecutor,*");
 * Set&lt;ObjectInstance&gt; s = mbs.queryMBeans(name, null);</pre>
 * </code>
 * You can use the PersistentExecutorMBean to manually perform fail over and to reassign tasks after a topology change.<br>
 * For example, to reassign all EJB timer tasks which have not yet ended from Liberty server myServer1 on
 * hostA.rchland.ibm.com to the instance that is active locally: <br>
 * <code>
 * <pre>
 * ObjectName fullName = s.iterator().next().getObjectName();
 * PersistentExecutorMBean proxy = JMX.newMXBeanProxy(mbs, fullName, PersistentExecutorMBean.class);
 * 
 * String[][] records = proxy.findPartitionInfo(
 * &nbsp; "hostA.rchland.ibm.com", null, "myServer1", "defaultEJBPersistentTimerExecutor");
 * long oldPartition = Long.valueOf(records[0][0]);
 * 
 * int numTransferred = proxy.transfer(null, oldPartition);
 * 
 * int count = proxy.removePartitionInfo(
 * &nbsp; "hostA.rchland.ibm.com", null, "myServer1", "defaultEJBPersistentTimerExecutor");</pre>
 * </code>
 * 
 * @ibm-api
 */

@MXBean
public interface PersistentExecutorMBean {

    //TODO Add in a future release 134442
//    public CompositeData findTask(long taskId);
//
//    public TabularData findTasks(String pattern, Character escape, String state, boolean inState, Long minId, Integer maxResults, String owner); //see findTaskStatus add owner parameter
//
//    public boolean cancelTask(long taskId);
//
//    public int cancelTasks(String pattern, Character escape, String state, boolean inState, String owner); // see remove
//
//    public boolean removeTask(long taskId);
//
//    public int removeTasks(String pattern, Character escape, String state, boolean inState, String owner);

    //public boolean suspend(long taskId);

    //public boolean resume(long taskId);
    /*
     * * Finds partition information in the persistent store. All of the parameters are optional.
     * If a parameter is specified, entries that match the parameter only are retrieved from the persistent store.
     * The executorIdentifier is a unique identifier for the Persistent Executor.
     * If the Persistent Executor is nested, the identifier is the display name.
     * For example:
     * <br>
     * <code> ejbContainer/timerService[default-0]/persistentExecutor[default-0] </code>
     * <br>
     * If the Persistent Executor is not nested, the identifier is the ID of the Persistent Executor.
     * If an ID is not specified for the Persistent Executor, then the identifier is the JNDI name of the Persistent Executor.
     */

    /**
     * Each task is partitioned to a persistent executor instance in a Liberty profile server with a particular
     * Liberty profile user directory on a host. The persistent executor instance runs only tasks that
     * are partitioned to it. This method finds partition information in the persistent store. All of
     * the parameters are optional. If a parameter is specified, only entries that match it are retrieved
     * from the persistent store. The executor identifier is a unique identifier for the Persistent Executor.
     * If the Persistent Executor is nested, this identifier is the display name. <br>
     * For example:
     * <br>
     * <code> ejbContainer/timerService[default-0]/persistentExecutor[default-0] </code>
     * <br>
     * If the Persistent Executor is not nested, the identifier is the ID of the Persistent Executor.
     * 
     * 
     * @param hostName the host name.
     * @param userDir wlp.user.dir
     * @param libertyServerName name of the Liberty server.
     * @param executorIdentifier Id or display name of the persistent executor.
     * @return a 2D array of partition information. Each subarray consists of
     *         (partition identifier, host name, Liberty profile user directory, Liberty server name, executor identifier)
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    public String[][] findPartitionInfo(String hostName, String userDir, String libertyServerName, String executorIdentifier) throws Exception;

    /**
     * Finds all task IDs for tasks that match the specified partition ID and the presence or absence
     * (as determined by the <code>inState</code> attribute) of the specified state.
     * <br>
     * For example, to find taskIDs for the first 100 tasks in partition 12 that have not completed all executions:
     * 
     * <code>
     * <pre> PersistentExecutorMBean proxy = JMX.newMXBeanProxy(mbs, name, PersistentExecutorMBean.class);
     * Long[] taskIds = proxy.findTaskIds(12, "ENDED", false, null, 100);</pre>
     * </code>
     * 
     * Options for state are: <code> "SCHEDULED", "UNATTEMPTED", "SKIPPED", "SKIPRUN_FAILED", "ENDED", "SUCCESSFUL", "FAILURE_LIMIT_REACHED", "CANCELED", "ANY" </code>
     * 
     * @param partition identifier of the partition in which to search for tasks.
     * @param state a task state.
     * @param inState indicates whether to include or exclude results with the specified state
     * @param minId minimum value for task id to be returned in the results. A null value means no minimum.
     * @param maxResults limits the number of results to return to the specified maximum value. A null value means no limit.
     * @return in-memory, ordered array of task ids.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    public Long[] findTaskIds(long partition, String state, boolean inState, Long minId, Integer maxResults) throws Exception;

    /**
     * Transfers tasks that have not yet ended to this persistent executor instance.
     * 
     * @param maxTaskId task ID including and up to which to transfer non-ended tasks from the old partition to this partition.
     *            If null, all non-ended tasks are transferred from the old partition to this partition.
     * @param oldPartitionId partition id to which tasks are currently assigned.
     * @return count of transferred tasks.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    public int transfer(Long maxTaskId, long oldPartitionId) throws Exception;

    /**
     * Removes partition information from the persistent store. All of the parameters are optional.
     * If a parameter is specified, only entries that match it are removed from the persistent store.
     * 
     * @param hostName the host name.
     * @param userDir wlp.user.dir
     * @param libertyServerName name of the Liberty server.
     * @param executorIdentifier Id or display name of the persistent executor.
     * @return the number of entries removed from the persistent store.
     * @throws Exception if an error occurs when attempting to access the persistent task store.
     */
    public int removePartitionInfo(String hostName, String userDir, String libertyServerName, String executorIdentifier) throws Exception;
}
