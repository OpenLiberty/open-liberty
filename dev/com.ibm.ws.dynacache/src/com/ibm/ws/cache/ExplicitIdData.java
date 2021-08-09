/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

public class ExplicitIdData {

    public Object id;               // cache id
    public byte   info;             // info

	public ExplicitIdData() {
	}

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("EID: id=");
        sb.append(id);
        sb.append(" info=");
        sb.append(info);
        return sb.toString();
    }
}
