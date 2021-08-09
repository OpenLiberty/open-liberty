/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.Trigger;

/**
 * Trigger that declares itself serializable but fails to serialize.
 */
public class TriggerThatFailsSerialization implements Serializable, Trigger {
    private static final long serialVersionUID = -5238148516697761849L;

    @Override
    public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
        return new Date();
    }

    @Override
    public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
        return false;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("Intentionally failing serialization");
    }
}