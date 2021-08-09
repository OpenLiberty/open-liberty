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

import java.util.Enumeration;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.json.JsonObject;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Helper class to set/get jms message property and jms message body
 * 
 */
@Trivial
public class BatchJmsMessageHelper {

	/**
	 * Set properties to jms message body
	 * 
	 * @param parameters
	 * @param jmsMsg
	 * @throws JMSException
	 */
	@SuppressWarnings("unchecked")
	public static void setJobParametersToJmsMessageBody(Properties parameters, MapMessage jmsMsg) throws JMSException {
		if (parameters != null) {
			Enumeration<String> e = (Enumeration<String>) parameters.propertyNames();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				String value = parameters.getProperty(key);
				// set property to message body
				jmsMsg.setString(key, value);
			}
		}
	}

	/**
	 * Set properties to jms message property if the name is valid jms identifier
	 * 
	 * @param parameters
	 * @param jmsMsg
	 * @throws JMSException
	 */
	@SuppressWarnings("unchecked")
	public static void setJobParametersToJmsMessageProperties(Properties parameters, Message jmsMsg) throws JMSException {
		if (parameters != null) {
			Enumeration<String> e = (Enumeration<String>) parameters.propertyNames();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				String value = parameters.getProperty(key);
				if (isValidJmsStringPropertyKey(key)) {
					// set property to jms message property
					jmsMsg.setStringProperty(key, value);
				}
			}
		}
	}

	/**
	 * Get user defined properties
	 * 
	 * @param msg
	 * @return
	 * @throws JMSException
	 */
	@SuppressWarnings("unchecked")
	public static Properties getJobParametersFromJmsMessage(MapMessage msg) throws JMSException {

		Properties jobParameters = new Properties();
		Enumeration<String> names = (Enumeration<String>) msg.getMapNames();
		while (names.hasMoreElements()) {
			String key = names.nextElement();
			// exclude any batch internal property
			if (!key.startsWith(BatchJmsConstants.INTERNAL_PREFIX)) {
				jobParameters.put(key, msg.getString(key));
			}
		}
		return jobParameters;
	}

	/**
	 * Determine if a string (identifier) can be set as Jms message property. A
	 * valid Jms message property name must be a valid java identifier.
	 * 
	 * An identifier is an unlimited-length sequence of letters and digits, the
	 * first of which must be a letter. A letter is any character for which the
	 * method Character.isJavaLetter returns true. This includes '_' and '$'. A
	 * letter or digit is any character for which the method
	 * Character.isJavaLetterOrDigit returns true.
	 * 
	 * Identifiers cannot be the names NULL, TRUE, and FALSE. Identifiers cannot
	 * be NOT, AND, OR, BETWEEN, LIKE, IN, IS, or ESCAPE.
	 * 
	 * @param key
	 * @return
	 */
	public static boolean isValidJmsStringPropertyKey(String key) {

		if (key == null || key.isEmpty()) {
			return false;
		}

		if (key.equalsIgnoreCase("NULL") 
		        || key.equalsIgnoreCase("TRUE")
				|| key.equalsIgnoreCase("FALSE") 
				|| key.equalsIgnoreCase("NOT")
				|| key.equalsIgnoreCase("AND") 
				|| key.equalsIgnoreCase("OR")
				|| key.equalsIgnoreCase("BETWEEN")
				|| key.equalsIgnoreCase("LIKE") 
				|| key.equalsIgnoreCase("IN")
				|| key.equalsIgnoreCase("IS") 
				|| key.equalsIgnoreCase("ESCAPE")) {

			return false;
		}
		char[] testChars = key.toCharArray();

		if (!Character.isJavaIdentifierStart(testChars[0])) {
			return false;
		}

		for (int i = 1; i < testChars.length; i++) {
			if (!Character.isJavaIdentifierPart(testChars[i])) {
				return false;
			}
		}
		return true;
	}
    
    /**
     * Populate the event message with data.
     * @param eventMsg JMS TextMessage type
     * @param jsonObject json representation of the data to be publish 
     * @throws JMSException
     */
    public static void setJobEventMessage(TextMessage eventMsg, JsonObject jsonObject) throws JMSException {
        //set major/minor version
        setMajorVersionToJmsMessage(eventMsg);
        setMinorVersionToJmsMessage(eventMsg);
        
        //set instance id property
        if (jsonObject.getJsonNumber("instanceId") != null) {
            long instanceId = jsonObject.getJsonNumber("instanceId").longValue();
            setInstanceIdToJobEventMessage(eventMsg, instanceId);
        }

        //set execution id property
        if (jsonObject.getJsonNumber("executionId") != null) {
            long executionId = jsonObject.getJsonNumber("executionId").longValue();
            setExecutionIdToJobEventMessage(eventMsg, executionId);
        }

        //set step execution id property
        if (jsonObject.getJsonNumber("stepExecutionId") != null) {
        	long stepExecutionId = jsonObject.getJsonNumber("stepExecutionId").longValue();
        	setStepExecutionIdToJobEventMessage(eventMsg, stepExecutionId);
        }
    	
    	//set message body
    	eventMsg.setText(jsonObject.toString());
        
    }
    
    public static String getJobEventMessage(TextMessage jmsMsg) throws JMSException{
        return jmsMsg.getText();        
    }
    
    /**
     * Set message major version
     * @param jmsMsg
     * @throws JMSException
     */
    public static void setMajorVersionToJmsMessage(TextMessage jmsMsg) throws JMSException {
        jmsMsg.setIntProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_EVENTS_MAJOR_VERSION, BatchJmsConstants.PROPERTY_VALUE_MESSAGE_EVENTS_MAJOR_VERSION);
    }
    
    /**
     * set message minor version
     * @param jmsMsg
     * @throws JMSException
     */
    public static void setMinorVersionToJmsMessage(TextMessage jmsMsg) throws JMSException {
        jmsMsg.setIntProperty(BatchJmsConstants.PROPERTY_NAME_MESSAGE_EVENTS_MINOR_VERSION, BatchJmsConstants.PROPERTY_VALUE_MESSAGE_EVENTS_MINOR_VERSION);
    }
    
    /**
     * set instance id
     * @param jmsMsg
     * @param intanceId
     * @throws JMSException
     */
    public static void setInstanceIdToJobEventMessage(TextMessage jmsMsg, long intanceId) throws JMSException {
        jmsMsg.setLongProperty(BatchJmsConstants.PROPERTY_NAME_JOB_INSTANCE_ID, intanceId);
    }
    
    /**
     * set execution id
     * @param jmsMsg
     * @param executionId
     * @throws JMSException
     */
    public static void setExecutionIdToJobEventMessage(TextMessage jmsMsg, long executionId) throws JMSException {
    	jmsMsg.setLongProperty(BatchJmsConstants.PROPERTY_NAME_JOB_EXECUTION_ID, executionId);
    }

    /**
     * set step execution id
     * @param jmsMsg
     * @param stepExecutionId
     * @throws JMSException
     */
    public static void setStepExecutionIdToJobEventMessage(TextMessage jmsMsg, long stepExecutionId) throws JMSException {
    	jmsMsg.setLongProperty(BatchJmsConstants.PROPERTY_NAME_STEP_EXECUTION_ID, stepExecutionId);
    }

    
}
