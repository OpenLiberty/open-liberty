/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.helloworldra;

import jakarta.resource.NotSupportedException;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.IndexedRecord;
import jakarta.resource.cci.MappedRecord;
import jakarta.resource.cci.RecordFactory;

public class HelloWorldRecordFactoryImpl implements RecordFactory {

    private static final String MAPPED_RECORD_NOT_SUPPORTED_ERROR = "Mapped record not supported";
    private static final String INVALID_RECORD_NAME = "Invalid record name";

    /**
     * Constructor for HelloWorldRecordFactoryImpl
     */
    public HelloWorldRecordFactoryImpl() {

        super();
    }

    /**
     * @see RecordFactory#createMappedRecord(String)
     */
    @Override
    public MappedRecord createMappedRecord(String recordName) throws ResourceException {

        throw new NotSupportedException(MAPPED_RECORD_NOT_SUPPORTED_ERROR);
    }

    /**
     * @see RecordFactory#createIndexedRecord(String)
     */
    @Override
    public IndexedRecord createIndexedRecord(String recordName) throws ResourceException {

        HelloWorldIndexedRecordImpl record = null;

        if ((recordName.equals(HelloWorldIndexedRecordImpl.INPUT))
            || (recordName.equals(HelloWorldIndexedRecordImpl.OUTPUT))) {
            record = new HelloWorldIndexedRecordImpl();
            record.setRecordName(recordName);
        }
        if (record == null) {
            throw new ResourceException(INVALID_RECORD_NAME);
        } else {
            return record;
        }
    }

}