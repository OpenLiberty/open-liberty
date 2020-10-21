/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package batch.fat.util;

/**
 * Did a copy paste of
 * com.ibm.jbatch.container.ws.JobInstance until I figure out how to copy that class into this FAT.
 * 
 * InstanceState for the JobInstance record.
 * 
 */
public enum InstanceStateMirrorImage {

    /**
     * The JobInstance has been submitted but not yet dispatched.
     */
    SUBMITTED,

    /**
     * The JobInstance has been queued to JMS, but not yet consumed by an endpoint.
     */
    JMS_QUEUED,

    /**
     * The JobInstance has been consumed by an endpoint, but a JobExecution
     * has not yet started.
     */
    JMS_CONSUMED,

    /**
     * The JobInstance has been dispatched and a JobExecution has been started.
     */
    DISPATCHED,

    /**
     * The JobInstance failed. This matches its BatchStatus.
     */
    FAILED,

    /**
     * The JobInstance has been stopped. This matches its BatchStatus.
     */
    STOPPED,

    /**
     * The JobInstance completed. This matches its BatchStatus.
     */
    COMPLETED,

    /**
     * The JobInstance was abandoned. This matches its BatchStatus.
     */
    ABANDONED;
}
