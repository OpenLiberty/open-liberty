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

import java.util.List;

public interface IEntity09 {

    //--------------------------------------------
    // Entity09 fields
    //--------------------------------------------
    public int getId();

    public void setId(int id);

    public String getEnt09_str01();

    public void setEnt09_str01(String str);

    public String getEnt09_str02();

    public void setEnt09_str02(String str);

    public String getEnt09_str03();

    public void setEnt09_str03(String str);

    public List<String> getEnt09_list01();

    public void setEnt09_list01(List<String> list);

    public List<Integer> getEnt09_list02();

    public void setEnt09_list02(List<Integer> list);

    public List<Embeddable01> getEnt09_list03();

    public void setEnt09_list03(List<Embeddable01> list);
}
