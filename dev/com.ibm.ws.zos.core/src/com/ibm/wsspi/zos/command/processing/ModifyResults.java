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
package com.ibm.wsspi.zos.command.processing;

import java.util.List;

/**
 * Encapsulates the results of processing a MVS modify command received
 * by a <code>CommandHandler</code>.
 */
public interface ModifyResults {

    /**
     * <code>List</code> of <code>String</code> Objects, each representing a
     * command response message. Message strings may be prefixed with a
     * message identifier (Note: method <code>responsesContainMSGIDs</code>
     * indicates the presence of a message identifier). The response message
     * text must be representable in IBM-1047. The messages language should be
     * English for consistency.
     *
     * <p>
     * If the responses are not prefixed with message identifier then the
     * command processing code will prefix each response with its own
     * message identifier.
     * <p>
     *
     * @return <code>List</code> of <code>String</code> Objects each representing a command
     *         response
     */
    public List<String> getResponses();

    /**
     * Set <code>List</code> of <code>String</code> Objects, each representing a
     * command response message. Message strings may be prefixed with a
     * message identifier (Note: method <code>responsesContainMSGIDs</code>
     * indicates the presence of a message identifier).
     * <p>
     * If the responses are not prefixed with message identifier then the
     * command processing code will prefix each response with its own
     * message identifier.
     * <p>
     *
     * @param responses
     *                      <code>List</code> of command response <code>String</code>
     *                      Objects
     *
     *                      The response message text must be representable in IBM-1047.
     *                      The messages language should be English for consistency.
     *
     */
    public void setResponses(List<String> responses);

    /**
     * Completion Status indicator. Indicates that the command was successfully
     * processed.
     */
    public final int PROCESSED_COMMAND = 1;

    /**
     * Completion Status indicator. Indicates that the command was unanticipated or unknown
     * to the <code>CommandHandler</code>.
     */
    public final int UNKNOWN_COMMAND = 2;

    /**
     * Completion Status indicator. Indicates that error was encountered while processing
     * the command.
     */
    public final int ERROR_PROCESSING_COMMAND = 3;

    /**
     * Property for determining if response is help text. When key is set to true, the
     * response will be treated as help text rather than a command response.
     */
    public final String HELP_RESPONSE_KEY = "help.response";

    /**
     * Set a completion status. Valid settings are: {@link com.ibm.wsspi.zos.command.processing.ModifyResults#PROCESSED_COMMAND},
     * {@link com.ibm.wsspi.zos.command.processing.ModifyResults#UNKNOWN_COMMAND} and {@link com.ibm.wsspi.zos.command.processing.ModifyResults#ERROR_PROCESSING_COMMAND}.
     *
     * @param completionStatus
     *                             completion status indicator representing the results of processing the
     *                             command
     */
    public void setCompletionStatus(int completionStatus);

    /**
     * Return the completion status indicator. Valid settings are: {@link com.ibm.wsspi.zos.command.processing.ModifyResults#PROCESSED_COMMAND},
     * {@link com.ibm.wsspi.zos.command.processing.ModifyResults#UNKNOWN_COMMAND} and {@link com.ibm.wsspi.zos.command.processing.ModifyResults#ERROR_PROCESSING_COMMAND}.
     *
     * @return completion status representing the results of processing the
     *         command
     */
    public int getCompletionStatus();

    /**
     * Returns the property given the <code>key</code> from the associated <code>ModifyResults</code>
     *
     * @param key
     *                key for the property
     *
     * @return property from the associated <code>ModifyResults</code>
     */
    public Object getProperty(String key);

    /**
     * Sets the property given the <code>key</code> from the associated <code>ModifyResults</code>
     *
     * @param key
     *                  key for the property
     * @param value
     *                  value for the property
     */
    public void setProperty(String key, Object value);

    /**
     * Indication that responses contain message identifiers or not.
     *
     * @return <code>true</code> if messages start with message identifiers, <code>false</code> otherwise.
     */
    public boolean responsesContainMSGIDs();

    /**
     * Set indication that responses contain message identifiers
     *
     * @param value
     *                  <code>true</code> indicates responses have message identifiers, <code>false</code>
     *                  otherwise.
     */
    public void setResponsesContainMSGIDs(boolean value);
}
