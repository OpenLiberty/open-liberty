/*******************************************************************************
 * Copyright (c) 1997, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.pmi.wire;

public interface WpdData extends java.io.Serializable {
    public static final long serialVersionUID = -8267987626962974625L;

    public long getTime();

    public int getId();

    public String toXML();

    public void combine(WpdData other);
}
