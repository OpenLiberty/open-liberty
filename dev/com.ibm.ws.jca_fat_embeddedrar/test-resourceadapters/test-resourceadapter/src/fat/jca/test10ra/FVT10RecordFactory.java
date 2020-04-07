/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.test10ra;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

public class FVT10RecordFactory implements RecordFactory {

    @Override
    public IndexedRecord createIndexedRecord(String recordName) throws ResourceException {
        FVT10IndexedRecord record = null;

        if ((recordName.equals(FVT10IndexedRecord.input))
            || (recordName.equals(FVT10IndexedRecord.output))) {
            record = new FVT10IndexedRecord();
            record.setRecordName(recordName);
        }
        if (record == null) {
            throw new ResourceException("The record name is invalid");
        } else {
            return record;
        }

    }

    @Override
    public MappedRecord createMappedRecord(String arg0) throws ResourceException {
        throw new NotSupportedException("Mapped Records are not Supported.");
    }

}
