/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.management;

import java.sql.SQLException;

/**
 * Indicates that a datasource was not found within the java:comp/env
 * namespace. It can also indicate that that there is no component
 * context on the thread.
 */
public class ComponentDataSourceNotFoundException extends SQLException
{
    private static final long serialVersionUID = -5167464619842164470L;

    public ComponentDataSourceNotFoundException(String reason)
    {
        super(reason);
    }

    public ComponentDataSourceNotFoundException()
    {
        super();
    }

}
