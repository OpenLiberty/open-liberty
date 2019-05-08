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
package com.ibm.ws.rest.handler.validator.jca;

import java.util.LinkedHashMap;

/**
 * This interface defines various methods for validating JMS connection factories and interpreting JMS exceptions.
 * An implementation of this interface will only be available when the JMS spec interfaces are available.
 */
public interface JMSValidator {
    /**
     * Returns the error code, if any, of the supplied JMSException.
     *
     * @param x a JMSException.
     * @return the error code, if any, of the supplied JMSException.
     */
    public String getErrorCode(Throwable x);

    /**
     * Returns true if the specified exception is an instance of javax.jms.JMSException, otherwise false.
     *
     * @param x the exception.
     * @return true if the specified exception is an instance of javax.jms.JMSException, otherwise false.
     */
    public boolean isJMSException(Throwable x);

    /**
     * Validates a JMS connection factory.
     *
     * @param cf       a JMS ConnectionFactory, QueueConnectionFactory, or TopicConnectionFactory.
     * @param user     user name, if any, for application authentication.
     * @param password password, if any, for application authentication.
     * @param result   key/value pairs to include in the validation result.
     * @throws Exception if an error occurs
     */
    public void validate(Object cf, String user, String password, LinkedHashMap<String, Object> result) throws Exception;
}