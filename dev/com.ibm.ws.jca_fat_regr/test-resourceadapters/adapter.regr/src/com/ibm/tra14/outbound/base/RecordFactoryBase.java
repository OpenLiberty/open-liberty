/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
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

package com.ibm.tra14.outbound.base;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

class RecordFactoryBase implements RecordFactory {
    public IndexedRecord createIndexedRecord(String recordName) throws ResourceException, NotSupportedException {
        throw new NotSupportedException("Indexed Records are not supported by this Resource Adapter");

    }

    public MappedRecord createMappedRecord(String recordName) throws ResourceException, NotSupportedException {
        throw new NotSupportedException("Mapped Records are not supported by this Resource Adapter");
    }
}
