/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.query.entities.interfaces;

import java.util.Date;

public interface IBusPass {
    public Date getExpires();

    public void setExpires(Date expires);

    public String getOwner();

    public void setOwner(String owner);

    @Override
    public String toString();

    public boolean equals(IBusPass arg);
}
