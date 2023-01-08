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

package com.ibm.ws.jpa.fvt.entity.entities;

import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;

public interface IAttributeConfigFieldEntity {
    public float getFloatValColumnPrecision();

    public void setFloatValColumnPrecision(float floatValColumnPrecision);

    public float getFloatValColumnScale();

    public void setFloatValColumnScale(float floatValColumnScale);

    public int getId();

    public void setId(int id);

    public int getIntValColumnName();

    public void setIntValColumnName(int intValColumnName);

    public int getIntValColumnTable();

    public void setIntValColumnTable(int intValColumnTable);

    public String getStringValColumnLength();

    public void setStringValColumnLength(String stringValColumnLength);

    public SerializableClass getNotNullable();

    public void setNotNullable(SerializableClass stringValColumnNullable);

    public String getStringValEager();

    public void setStringValEager(String stringValEager);

    public String getStringValLazy();

    public void setStringValLazy(String stringValLazy);

    public String getStringValOptional();

    public void setStringValOptional(String stringValOptional);

    public String getUniqueString();

    public void setUniqueString(String uniqueString);

    public String getUniqueConstraintString();

    public void setUniqueConstraintString(String uniqueConstraintString);
}
