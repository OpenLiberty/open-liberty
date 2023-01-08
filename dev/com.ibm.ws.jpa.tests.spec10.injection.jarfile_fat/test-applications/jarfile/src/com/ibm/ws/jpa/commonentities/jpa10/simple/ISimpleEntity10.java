/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.commonentities.jpa10.simple;

import java.io.Serializable;

public interface ISimpleEntity10 extends Serializable {
    public int getId();

    public void setId(int id);

    public String getStrData();

    public void setStrData(String strData);

    public char getCharData();

    public void setCharData(char charData);

    public int getIntData();

    public void setIntData(int intData);

    public long getLongData();

    public void setLongData(long longData);

    public float getFloatData();

    public void setFloatData(float floatData);

    public double getDoubleData();

    public void setDoubleData(double doubleData);

    public byte[] getByteArrData();

    public void setByteArrData(byte[] byteArrData);
}
