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

import java.io.Serializable;

public interface ISerializableClass extends Serializable {
    public byte[] getSomeBytes();

    public void setSomeBytes(byte[] someBytes);

    public int getSomeInt();

    public void setSomeInt(int someInt);

    public String getSomeString();

    public void setSomeString(String someString);

    @Override
    public boolean equals(Object o);

    @Override
    public String toString();
}
