/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.tra.ann;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

@SuppressWarnings("serial")
public class ConfigPropertyConnectionFactory implements javax.resource.cci.ConnectionFactory {

    public Connection getConnection() throws ResourceException {
        return null;
    }

    public Connection getConnection(ConnectionSpec arg0) throws ResourceException {
        return null;
    }

    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }

    public void setReference(Reference arg0) {
    }

    public Reference getReference() throws NamingException {
        return null;
    }

}
