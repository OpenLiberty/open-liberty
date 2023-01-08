/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
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
