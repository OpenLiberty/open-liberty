/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jbatch.jms.internal;

/**
 * Contains constants that being use in com.ibm.ws.jbatch.jms projects. TODO:
 * Make activation spec id, and queue name configurable.
 */
public class BatchJmsConstants {

    /**
     * Constants to construct a logical J2EE representation for batch activation
     * spec.
     */
    public static final String J2EE_APP_NAME = "JBatchListenerApp";
    public static final String J2EE_APP_MODULE = "JBatchListenerModule";
    public static final String J2EE_APP_COMPONENT = "JBatchListenerComp";

    /**
     * Batch JMS listener class name
     */
    public static final String JBATCH_JMS_LISTENER_CLASS_NAME = "com.ibm.ws.jbatch.jms.listener.BatchJmsEndpointListener";

    /**
     * Properties for dispatcher Jms message. Currently, appName is use as
     * message selector.
     * These are exposed to customer 
     */
    public static final String PROPERTY_NAME_APP_NAME = "com_ibm_ws_batch_applicationName";
    public static final String PROPERTY_NAME_MODULE_NAME = "com_ibm_ws_batch_moduleName";
    public static final String PROPERTY_NAME_COMP_NAME = "com_ibm_ws_batch_componentName";
	public static final String PROPERTY_NAME_STEP_NAME = "com_ibm_ws_batch_stepName";
	public static final String PROPERTY_NAME_PARTITION_NUM = "com_ibm_ws_batch_partitionNum";
    public static final String PROPERTY_NAME_WORK_TYPE = "com_ibm_ws_batch_work_type";

    /**
     * Parameters to set JMS message properties.  External to customers.
     */
    public static final String PROPERTY_NAME_MESSAGE_PRIORITY = "com_ibm_ws_batch_message_priority";
    public static final String PROPERTY_NAME_MESSAGE_DELIVERYDELAY = "com_ibm_ws_batch_message_deliveryDelay";

    /**
     * Prefix used endpoint to filter for container property
     */
    public static final String INTERNAL_PREFIX = "com_ibm_ws_batch_internal";
    /**
     * internal properties.  These are used by the endpoint, not needed for routing
     */
    public static final String PROPERTY_NAME_APP_TAG = INTERNAL_PREFIX + "_submitter";
    public static final String PROPERTY_NAME_JOB_INSTANCE_ID = INTERNAL_PREFIX + "_jobInstanceId";
    public static final String PROPERTY_NAME_JOB_NAME = INTERNAL_PREFIX + "_jobName";
    public static final String PROPERTY_NAME_JOB_XML_NAME = INTERNAL_PREFIX + "_jobXMLName";
    public static final String PROPERTY_NAME_SECURITY_CONTEXT = INTERNAL_PREFIX + "_securityContext";
    public static final String PROPERTY_NAME_JOB_EXECUTION_ID = INTERNAL_PREFIX + "_jobExecutionId";
    public static final String PROPERTY_NAME_STEP_EXECUTION_ID = INTERNAL_PREFIX + "_stepExecutionId";
    public static final String PROPERTY_NAME_JOB_OPERATION = INTERNAL_PREFIX + "_jobOperation";

   public static final String PROPERTY_NAME_JOB_CONTEXT = INTERNAL_PREFIX + "_jobContext";
   public static final String PROPERTY_NAME_STEP = INTERNAL_PREFIX + "_step";
    
    
    /**
     * Possible value for job operation property. Use in Jms message from
     * dispatcher to endpoint.
     */
    public static final String PROPERTY_VALUE_JOB_OPERATION_START = "Start";
    public static final String PROPERTY_VALUE_JOB_OPERATION_RESTART = "Restart";    
    public static final String PROPERTY_VALUE_JOB_OPERATION_START_PARTITION = "PartitionExecute";

    
    /*
     * Values for the workType. Use in Jms message from dispatcher to endpoint.
     * TODO: add splitFlows here when we implement remote split-flows. 
     */
    public static final String PROPERTY_VALUE_WORK_TYPE_JOB = "Job"; 
    public static final String PROPERTY_VALUE_WORK_TYPE_PARTITION = "Partition";

    /**
     * This is the value of
     * javax.enterprise.concurrent.ManagedTask.IDENTITY_NAME, but is hard-coded
     * here to avoid a dependency on the concurrency feature.
     */
    public static final String MANAGEDTASK_IDENTITY_NAME = "javax.enterprise.concurrent.IDENTITY_NAME";
    

    
    /**
     * Major version of dispatcher message
     * major number is a message attribute so it can be used as message selector
     * It is change if there is update to message that the lower level listener can not ignore
     */
    public static final int PROPERTY_VALUE_MESSAGE_MAJOR_VERSION = 1;
    public static final String PROPERTY_NAME_MESSAGE_MAJOR_VERSION = "com_ibm_ws_batch_majorVersion";
    
    /**
     * Minor version of dispatcher message
     *  minor version goes in the message body.
     *  It is change when there is update to down level listener can ignore
     */
    public static final int PROPERTY_VALUE_MESSAGE_MINOR_VERSION = 2;
    public static final String PROPERTY_NAME_MESSAGE_MINOR_VERSION = INTERNAL_PREFIX + "_minorVersion";

    /**
     * Major and Minor version of job events messages
     * Note: We used to use the same constant as the dispatcher message versioning and accidentally
     * 		 used 2.2 for the events version for a while. They have now been separated.
     * 
     * The major version number is a message attribute so it can be used as a message selector.
     * It is changed if there is an update to the message that the lower level listener can not ignore
     * 
     * The minor version goes in the message body.
     * It is changed when there is an update that the down level listener can ignore
     * 
     */
    public static final int PROPERTY_VALUE_MESSAGE_EVENTS_MAJOR_VERSION = 1;
    public static final String PROPERTY_NAME_MESSAGE_EVENTS_MAJOR_VERSION = "com_ibm_ws_batch_events_majorVersion";
    public static final int PROPERTY_VALUE_MESSAGE_EVENTS_MINOR_VERSION = 0;
    public static final String PROPERTY_NAME_MESSAGE_EVENTS_MINOR_VERSION = INTERNAL_PREFIX + "events_minorVersion";

    
    /**
     * Property names of batchJmsDispatcher and batchJmsEndpoint config
     * 
     * <batchJmsDispatcher connectionFactoryRef="batchCconnectionFactory"
     * queueRef="batchJobSubmissionQueue"/>
     * 
     * 
     * <batchJmsExecutor activationSpecRef="batchActivationSpec"
     * queueRef="batchJobSubmissionQueue"/>
     * 
     */

    public static final String QUEUE_REF_CONFIG = "queueRef";
    public static final String ACTIVATION_SPEC_REF_CONFIG = "activationSpecRef"; 
}
