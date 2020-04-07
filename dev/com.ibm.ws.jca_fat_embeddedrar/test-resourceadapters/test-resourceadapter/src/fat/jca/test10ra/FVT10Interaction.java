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

import java.util.Date;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.ResourceWarning;

public class FVT10Interaction implements Interaction {

    private FVT10Connection connection;

    public FVT10Interaction(FVT10Connection connection) {
        this.connection = connection;
    }

    @Override
    public void clearWarnings() throws ResourceException {

    }

    @Override
    public void close() throws ResourceException {
        connection = null;
    }

    @Override
    public Record execute(InteractionSpec arg0, Record arg1) throws ResourceException {
        throw new NotSupportedException("This is not supported");
    }

    @Override
    public boolean execute(InteractionSpec iSpec, Record input, Record output) throws ResourceException {
        if (connection != null) {
            if (iSpec == null) {
                if (input.getRecordName().equals(FVT10IndexedRecord.input)) {
                    if (output.getRecordName()
                                    .equals(FVT10IndexedRecord.output)) {
                        ((FVT10IndexedRecord) output).clear();
                        ((FVT10IndexedRecord) output).add(new Date());
                    } else {
                        throw new ResourceException("Invalid output record passed.");
                    }
                } else {
                    throw new ResourceException("Invalid input record passed");
                }
            } else if (((FVT10InteractionSpec) iSpec).getFunctionName()
                            .equals(
                                    FVT10InteractionSpec.RETURN_DATE)) {
                if (input.getRecordName().equals(FVT10IndexedRecord.input)) {
                    if (output.getRecordName()
                                    .equals(FVT10IndexedRecord.output)) {
                        ((FVT10IndexedRecord) output).clear();
                        ((FVT10IndexedRecord) output).add(new Date());
                    } else {
                        throw new ResourceException("Invalid output record passed.");
                    }
                } else {
                    throw new ResourceException("Invalid input record passed");
                }

            } else {
                throw new ResourceException("Unsupported funtion name");
            }

        } else {
            throw new ResourceException("The interaction is closed.");
        }
        return false;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public ResourceWarning getWarnings() throws ResourceException {
        return null;
    }

}
