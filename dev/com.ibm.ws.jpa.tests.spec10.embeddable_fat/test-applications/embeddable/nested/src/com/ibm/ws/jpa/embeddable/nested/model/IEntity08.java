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

import java.util.Set;

public interface IEntity08 {

    //--------------------------------------------
    // Entity08 fields
    //--------------------------------------------
    public int getId();

    public void setId(int id);

    public String getEnt08_str01();

    public void setEnt08_str01(String str);

    public String getEnt08_str02();

    public void setEnt08_str02(String str);

    public String getEnt08_str03();

    public void setEnt08_str03(String str);

    public Set<String> getEnt08_set01();

    public void setEnt08_set01(Set<String> set);

    public Set<Integer> getEnt08_set02();

    public void setEnt08_set02(Set<Integer> set);

    public Set<Embeddable01> getEnt08_set03();

    public void setEnt08_set03(Set<Embeddable01> set);
}
