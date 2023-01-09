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

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionMetaData;
import javax.resource.cci.Interaction;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.ResultSetInfo;

public class ConfigPropertyConnection2 implements Connection {

    public void close() throws ResourceException {
    }

    public Interaction createInteraction() throws ResourceException {
        return null;
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    public ConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    public ResultSetInfo getResultSetInfo() throws ResourceException {
        return null;
    }

}
