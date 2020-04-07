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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionManager;

public class FVT10ConnectionFactory implements ConnectionFactory {

    private static final long serialVersionUID = 1233455442342341L;
    private Reference reference;
    private final ConnectionManager cm;
    private final FVT10ManagedConnectionFactory mcf;

    public FVT10ConnectionFactory(ConnectionManager cm, FVT10ManagedConnectionFactory mcf) {
        this.cm = cm;
        this.mcf = mcf;
    }

    @Override
    public void setReference(Reference ref) {
        reference = ref;
    }

    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }

    @Override
    public Connection getConnection() throws ResourceException {
        FVT10ConnectionRequestInfo cri = new FVT10ConnectionRequestInfo(mcf.getUserName(), mcf.getPassword());
        return ((FVT10Connection) cm.allocateConnection(mcf, cri)).init(cm);
    }

    @Override
    public Connection getConnection(ConnectionSpec arg0) throws ResourceException {
        throw new NotSupportedException();
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return new FVT10ResourceAdapterMetadataImpl();
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return new FVT10RecordFactory();
    }

}
