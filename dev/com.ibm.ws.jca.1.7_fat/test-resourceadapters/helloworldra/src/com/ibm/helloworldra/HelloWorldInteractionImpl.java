/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

public class HelloWorldInteractionImpl implements Interaction {

    private static final String CLOSED_ERROR = "Connection closed";
    private static final String INVALID_FUNCTION_ERROR = "Invalid function";
    private static final String INVALID_INPUT_ERROR = "Invalid input record for function";
    private static final String INVALID_OUTPUT_ERROR = "Invalid output record for function";
    private static final String OUTPUT_RECORD_FIELD_01 = "Hello World!";
    private static final String EXECUTE_WITH_INPUT_RECORD_ONLY_NOT_SUPPORTED = "execute() with input record only not supported";

    private Connection connection;
    private boolean valid;

    /**
     * Constructor for HelloWorldInteractionImpl
     */
    public HelloWorldInteractionImpl(Connection connection) {

        super();
        this.connection = connection;
        valid = true;
    }

    /**
     * @see Interaction#close()
     */
    @Override
    public void close() throws ResourceException {

        connection = null;
        valid = false;
    }

    /**
     * @see Interaction#getConnection()
     */
    @Override
    public Connection getConnection() {

        return connection;
    }

    /**
     * @see Interaction#execute(InteractionSpec, Record, Record)
     */
    @Override
    public boolean execute(InteractionSpec ispec, Record input, Record output) throws ResourceException {

        if (valid) {
            if (((HelloWorldInteractionSpecImpl) ispec)
                            .getFunctionName()
                            .equals(HelloWorldInteractionSpec.SAY_HELLO_FUNCTION)) {
                if (input.getRecordName().equals(HelloWorldIndexedRecord.INPUT)) {
                    if (output.getRecordName().equals(HelloWorldIndexedRecord.OUTPUT)) {
                        ((HelloWorldIndexedRecord) output).clear();
                        ((HelloWorldIndexedRecord) output).add(OUTPUT_RECORD_FIELD_01);
                    } else {
                        throw new ResourceException(INVALID_OUTPUT_ERROR);
                    }
                } else {
                    throw new ResourceException(INVALID_INPUT_ERROR);
                }

            } else {
                throw new ResourceException(INVALID_FUNCTION_ERROR);
            }
        } else {
            throw new ResourceException(CLOSED_ERROR);
        }
        return true;
    }

    /**
     * @see Interaction#execute(InteractionSpec, Record)
     */
    @Override
    public Record execute(InteractionSpec ispec, Record input) throws ResourceException {

        throw new NotSupportedException(EXECUTE_WITH_INPUT_RECORD_ONLY_NOT_SUPPORTED);
    }

    /**
     * @see Interaction#getWarnings()
     */
    @Override
    public ResourceWarning getWarnings() throws ResourceException {

        return null;
    }

    /**
     * @see Interaction#clearWarnings()
     */
    @Override
    public void clearWarnings() throws ResourceException {
    }

}