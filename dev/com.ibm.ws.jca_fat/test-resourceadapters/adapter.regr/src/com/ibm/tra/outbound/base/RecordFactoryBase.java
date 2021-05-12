/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra.outbound.base;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

class RecordFactoryBase implements RecordFactory {
    @Override
    public IndexedRecord createIndexedRecord(String recordName) throws ResourceException, NotSupportedException {
        throw new NotSupportedException("Indexed Records are not supported by this Resource Adapter");

    }

    @Override
    public MappedRecord createMappedRecord(String recordName) throws ResourceException, NotSupportedException {
        throw new NotSupportedException("Mapped Records are not supported by this Resource Adapter");
    }
}
