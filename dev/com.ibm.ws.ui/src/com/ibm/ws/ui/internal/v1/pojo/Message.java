/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import com.ibm.ws.ui.persistence.InvalidPOJOException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * Class design requirements:
 * 1. JSONable POJO. We leverage Jackson to serialize this object into JSON format.
 * 2. Container of error message data returned to the client.
 */
public class Message implements SelfValidatingPOJO {

    /**
     * HTTP status code.
     * Required field. Must not be zero.
     */
    private int status;

    /**
     * Translated message with message ID.
     * Required field. Must not be null.
     */
    private String message;

    /**
     * Translated user message.
     * Optional field. May be null.
     */
    private String userMessage;

    /**
     * Translated develop message.
     * Optional field. May be null.
     */
    private String developerMessage;

    /**
     * Initialize the ErrorMessage to return.
     * Only the required fields must be provided. Additional entries can be set via the appropriate setters.
     * 
     * @param status
     * @param message
     */
    public Message(final int status, final String message) {
        this.status = status;
        this.message = message;
        this.userMessage = null;
        this.developerMessage = null;
    }

    /**
     * Zero-argument constructor used by Jackson.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private Message() {
        // No initialization of internal data structures as Jackson will set them via setters.
    }

    /**
     * Retrieve the HTTP status code.
     * 
     * @return the HTTP status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private void setStatus(final int status) {
        this.status = status;
    }

    /**
     * Retrieve the translated error message.
     * 
     * @return the translated error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Setter used by Jackson when deserializing.
     * Intentionally private visibility.
     */
    @SuppressWarnings("unused")
    private void setMessage(final String message) {
        this.message = message;
    }

    /**
     * Retrieve the informational message intended for a user.
     * 
     * @return the info message for a user
     */
    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Set the informational message intended for a user.
     * 
     * @param userMessage the info message for a user
     */
    public void setUserMessage(final String userMessage) {
        this.userMessage = userMessage;
    }

    /**
     * Retrieve the informational message intended for a developer
     * .
     * 
     * @return the info message for a developer
     */
    public String getDeveloperMessage() {
        return developerMessage;
    }

    /**
     * Set the informational message intended for a developer.
     * 
     * @param userMessage the info message for a developer
     */
    public void setDeveloperMessage(final String developerMessage) {
        this.developerMessage = developerMessage;
    }

    /**
     * Validates the ErrorMessage is complete. An incomplete ErrorMessage would
     * be one which has no status (or negative status) and/or no message.
     * 
     * @return {@code true} if the ErrorMessage is valid, or {@code false} if required fields are missing
     */
    @Override
    public void validateSelf() throws InvalidPOJOException {
        if (status <= 0) {
            throw new InvalidPOJOException("ErrorMessage is not valid, 'status' is zero or negative");
        }
        if (message == null) {
            throw new InvalidPOJOException("ErrorMessage is not valid, 'message' field is null");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Message) {
            Message that = (Message) o;

            boolean sameFields = true;
            sameFields &= (this.status == that.status);
            sameFields &= (this.message == that.message) || (this.message != null && (this.message.equals(that.message)));
            sameFields &= (this.userMessage == that.userMessage) || (this.userMessage != null && (this.userMessage.equals(that.userMessage)));
            sameFields &= (this.developerMessage == that.developerMessage) || (this.developerMessage != null && (this.developerMessage.equals(that.developerMessage)));

            return sameFields;
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc} <p>
     * An error message with no status or message is not valid.
     * Therefore we don't really need to worry about the hash code.
     */
    @Override
    public int hashCode() {
        return this.status + ((this.message == null) ? 0 : this.message.hashCode());
    }

    @Override
    public String toString() {
        return "{status: " + status + ", message: " + message + ", useMessage: " + userMessage + ", developerMessage: " + developerMessage + "}";
    }

}
