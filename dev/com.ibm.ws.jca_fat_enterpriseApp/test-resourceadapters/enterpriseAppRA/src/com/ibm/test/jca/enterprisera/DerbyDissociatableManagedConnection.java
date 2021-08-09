/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.jca.enterprisera;

import javax.resource.ResourceException;
import javax.resource.spi.DissociatableManagedConnection;
import javax.security.auth.Subject;

public class DerbyDissociatableManagedConnection extends DerbyManagedConnection implements DissociatableManagedConnection {

    DerbyDissociatableManagedConnection(DerbyManagedConnectionFactory mcf, DerbyConnectionRequestInfo cri, Subject subj) throws ResourceException {
        super(mcf, cri, subj);
    }

    /** {@inheritDoc} */
    @Override
    public void dissociateConnections() throws ResourceException {
        for (Object handle : handles)
            ((DerbyConnection) handle).mc = null;
    }
}
