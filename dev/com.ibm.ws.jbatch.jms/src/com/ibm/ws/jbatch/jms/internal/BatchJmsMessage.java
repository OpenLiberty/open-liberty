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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Properties;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import com.ibm.jbatch.container.ws.PartitionReplyMsg;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.jbatch.jms.internal.dispatcher.StartPartitionPayload;

/**
 * Convenience wrapper around a jms message.
 */
public class BatchJmsMessage {
    
    /**
     * The JMS message.
     */
    private Message message;

    /**
     * CTOR.
     */
    public BatchJmsMessage(Message message) {
        this.message = message;
    }
    
    /**
     * Set major version for message.
     * 
     * @param jmsMsg
     * @throws JMSException
     */
    public BatchJmsMessage setVersion() throws JMSException {
        message.setIntProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_MAJOR_VERSION,
                               BatchJmsConstants.PROPERTY_VALUE_MESSAGE_MAJOR_VERSION);
        
        message.setIntProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_MINOR_VERSION,
                               BatchJmsConstants.PROPERTY_VALUE_MESSAGE_MINOR_VERSION);
        return this;
    }

    /**
     * @return this
     */
    public BatchJmsMessage setJ2eeName(J2EEName j2eeName) throws JMSException {
        message.setStringProperty(BatchJmsConstants.PROPERTY_NAME_APP_NAME, j2eeName.getApplication());
        message.setStringProperty(BatchJmsConstants.PROPERTY_NAME_MODULE_NAME, j2eeName.getModule());
        message.setStringProperty(BatchJmsConstants.PROPERTY_NAME_COMP_NAME, j2eeName.getComponent());
        return this;
    }

    /**
     * @return this
     */
    public BatchJmsMessage setInstanceId(long jobInstanceId) throws JMSException {
        message.setLongProperty(BatchJmsConstants.PROPERTY_NAME_JOB_INSTANCE_ID, jobInstanceId);
        return this;
    }
    
    /**
     * @return this
     */
    public BatchJmsMessage setExecutionId(long executionId) throws JMSException {
        message.setLongProperty(BatchJmsConstants.PROPERTY_NAME_JOB_EXECUTION_ID, executionId);
        return this;
    }

    /**
     * @return this
     */
    public BatchJmsMessage setOperation(String operation) throws JMSException {
        message.setStringProperty(BatchJmsConstants.PROPERTY_NAME_JOB_OPERATION, operation);
        return this;
    }
    
    /**
     * @return this
     * property not used by the listener. It is just used in the ActivationSpec MessageSelector
     */
    public BatchJmsMessage setStepName(String stepName) throws JMSException {
		message.setStringProperty(BatchJmsConstants.PROPERTY_NAME_STEP_NAME, stepName);
		return this;
	}

    /**
     * @return this
     */
    public BatchJmsMessage setJobParameters(Properties jobParameters) throws JMSException {
        BatchJmsMessageHelper.setJobParametersToJmsMessageProperties(jobParameters, message);
        
        if (message instanceof MapMessage) {
            BatchJmsMessageHelper.setJobParametersToJmsMessageBody(jobParameters, (MapMessage) message);
        }
        
        return this;
    }
    
    /**
     * Set security context to jms message
     * 
     * @return this
     */
    public BatchJmsMessage setSecurityContext(byte[] securityContext) throws JMSException {
        if (message instanceof MapMessage) {
            ((MapMessage)message).setBytes(BatchJmsConstants.PROPERTY_NAME_SECURITY_CONTEXT, securityContext);
        } else {
            throw new IllegalStateException("Cannot set securityContext for non-MapMessage JMS message: " + message);
        }
        return this;
    }

    /**
     * @return the JMS message
     */
    public Message getMessage() {
        return message;
    }

    /**
     * @return the app name
     */
    public String getApplicationName() throws JMSException {
        return message.getStringProperty(BatchJmsConstants.PROPERTY_NAME_APP_NAME);
    }

    /**
     * @return the operation
     */
    public String getOperation() throws JMSException {
        String op = message.getStringProperty(BatchJmsConstants.PROPERTY_NAME_JOB_OPERATION);
        return (op == null) ? "" : op;
    }
    
    /**
     * @return the operation
     */
    public String getOperationUnchecked() {
        try {
            return getOperation();
        } catch (JMSException e) {
            // ffdc
        }
        return "";
    }

    /**
     * @return the instance id
     */
    public long getInstanceId() throws JMSException {
        return message.getLongProperty(BatchJmsConstants.PROPERTY_NAME_JOB_INSTANCE_ID);
    }
    
    /**
     * @return the instance id
     */
    public long getInstanceIdUnchecked() {
        try {
            return getInstanceId();
        } catch (JMSException e) {
            // ffdc
        }
        return -1;
    }

    /**
     * @return the execution id
     */
    public long getExecutionId() throws JMSException {
        return message.getLongProperty(BatchJmsConstants.PROPERTY_NAME_JOB_EXECUTION_ID);
    }

    /**
     * @return the security context.
     */
    public byte[] getSecurityContext() throws JMSException {
        if (message instanceof MapMessage) {
            return ((MapMessage)message).getBytes(BatchJmsConstants.PROPERTY_NAME_SECURITY_CONTEXT);
        } else {
            throw new IllegalStateException("Cannot retrieve security context from non-MapMessage message: " + message);
        }
    }

    /**
     * @return job parameters
     */
    public Properties getJobParameters() throws JMSException {
        if (message instanceof MapMessage) {
            return BatchJmsMessageHelper.getJobParametersFromJmsMessage((MapMessage)message);
        } else {
            throw new IllegalStateException("Cannot retrieve job parameters from non-MapMessage message: " + message);
        }
    }

    /**
     * @return stringified
     */
    public String toString() {
        return "BatchJmsMessage:" + message.toString();
    }

    /**
     * @return the partition num
     */
    public int getPartitionNumber() throws JMSException {
        return message.getIntProperty(BatchJmsConstants.PROPERTY_NAME_PARTITION_NUM);
    }

    /**
     * @return this
     */
    public BatchJmsMessage setPartitionNumber(int partitionNum) throws JMSException {
        message.setIntProperty(BatchJmsConstants.PROPERTY_NAME_PARTITION_NUM, partitionNum);
        return this;
    }

    /**
     * @return this
     */
    public BatchJmsMessage setJmsReplyToQueue(Destination replyToQueue) throws JMSException {
        message.setJMSReplyTo(replyToQueue);
        return this;
    }

    /**
     * Set the serialized object for the ObjectMessage.
     * 
     * @return this
     */
    public BatchJmsMessage setObjectPayload( Serializable payload ) throws JMSException {
        ((ObjectMessage)message).setObject( payload );
        return this;
    }
    
    /**
     * @return the serialized payload as a StartPartitionPayload object.
     */
    public Serializable getObjectPayload() throws JMSException {
        return ((ObjectMessage)message).getObject();
    }
    
    /**
     * The StartPartitionPayload is first serialized to a byte[].
     * The byte[] is serialized into the JMS ObjectMessage payload.
     * This is to avoid classloading issues, as the jms bundle cannot
     * load the StartPartitionPayload class during deser.  So instead,
     * the payload is deserialized to a byte[], then the byte[] is
     * deserialized here into a StartPartitionPayload object.
     * 
     */
    public BatchJmsMessage setStartPartitionPayload(StartPartitionPayload payload) throws JMSException, IOException {
        setObjectPayload( serializeToBytes(payload) );
        return this;
    }
    
    /**
     * @return the serialized payload as a StartPartitionPayload object.
     */
    public StartPartitionPayload getStartPartitionPayload() throws JMSException, IOException, ClassNotFoundException {
        return (StartPartitionPayload) deserializeFromBytes( (byte[]) getObjectPayload() );
    }
    
    /**
     * @return the serialized payload as a byte array
     */
    private byte[] serializeToBytes(Serializable payload)  {
    	byte[] retVal = null;
    	try {
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		ObjectOutputStream oos = null;
    		try {
    			oos = new ObjectOutputStream(baos);
    			oos.writeObject(payload);
    			retVal = baos.toByteArray();

    		} catch (IOException e) {
    			throw new IllegalStateException("Cannot serialize the message payload");
    		}
    		finally {
    			oos.close();
			}
    	}catch (IOException e) {
			throw new IllegalStateException("Cannot serialize the message payload");
			}
        return retVal;
    }
    
    /**
     * @return the de-serialized object 
     */
    private Object deserializeFromBytes(byte[] bytes)  {
    	Object retVal = null;
//        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//        return new ObjectInputStream(bais).readObject();
//
		try{
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(bais);
				retVal =  ois.readObject();
			} finally {
				ois.close();
				}	
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Problem while trying to deserialize the payload");
		} catch (IOException e) {
			throw new IllegalStateException("Problem while trying to deserialize the payload");
		}
		return retVal;
    }

    /**
     * @return the JSMReplyToQueue
     */
    public Destination getJmsReplyToQueue() throws JMSException {
        return message.getJMSReplyTo();
    }

    /**
     * @return a BatchJmsMessage which contains a serialized PartitionReplyMsg
     */
    public BatchJmsMessage setPartitionReplyMsgPayload(PartitionReplyMsg payload) throws JMSException, IOException {
        setObjectPayload( serializeToBytes(payload) );
        return this;
    }
    
    /**
     * @return the serialized payload as a PartitionReplyMsg object.
     */
    public PartitionReplyMsg getPartitionReplyMsgPayload() throws JMSException, IOException, ClassNotFoundException {
        return (PartitionReplyMsg) deserializeFromBytes( (byte[]) getObjectPayload() );
    }
    
    /*
     * @return this
     */
    public BatchJmsMessage setBatchWorkType(String workType) throws JMSException{
    	message.setStringProperty(BatchJmsConstants.PROPERTY_NAME_WORK_TYPE, workType);
    	return this;
    }

    /*
     * @return The batch work type
     */
    public String getBatchWorkType() throws JMSException{
    	return message.getStringProperty(BatchJmsConstants.PROPERTY_NAME_WORK_TYPE);
    }
    
 }
