/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.nested.model;

public interface IEntity02 {

    //--------------------------------------------
    // Entity02 fields
    //--------------------------------------------
    public int getId();

    public void setId(int id);

    public String getEnt02_str01();

    public void setEnt02_str01(String str);

    public String getEnt02_str02();

    public void setEnt02_str02(String str);

    public String getEnt02_str03();

    public void setEnt02_str03(String str);

    //--------------------------------------------
    // Embeddable02a fields
    //--------------------------------------------
    public int getEmb02a_int01();

    public void setEmb02a_int01(int ii);

    public int getEmb02a_int02();

    public void setEmb02a_int02(int ii);

    public int getEmb02a_int03();

    public void setEmb02a_int03(int ii);

    //--------------------------------------------
    // Embeddable02b fields
    //--------------------------------------------
    public int getEmb02b_int04();

    public void setEmb02b_int04(int ii);

    public int getEmb02b_int05();

    public void setEmb02b_int05(int ii);

    public int getEmb02b_int06();

    public void setEmb02b_int06(int ii);
}
