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
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionManager;

public class FVT10Connection implements Connection {
    ConnectionManager cm;
    FVT10ManagedConnection mc;

    FVT10Connection(FVT10ManagedConnection mc) {
        this.mc = mc;
    }

    FVT10Connection init(ConnectionManager cm) {
        this.cm = cm;
        return this;
    }

    @Override
    public void close() {
        if (mc != null)
            mc.notify(ConnectionEvent.CONNECTION_CLOSED, this, null);
        mc = null;
    }

    @Override
    public Interaction createInteraction() throws ResourceException {
        if (mc == null) {
            throw new ResourceException("The connection is closed.");
        }
        return new FVT10Interaction(this);
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("Transactions not supported");
    }

    @Override
    public ConnectionMetaData getMetaData() throws ResourceException {
        return new FVT10ConnectionMetadata(mc.mcf.getUserName());
    }

    @Override
    public ResultSetInfo getResultSetInfo() throws ResourceException {
        throw new NotSupportedException("Result Sets not supported");
    }

}
