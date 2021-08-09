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

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public interface IEntity11 {

    //--------------------------------------------
    // Entity11 fields
    //--------------------------------------------
    public int getId();

    public void setId(int id);

    public String getEnt11_str01();

    public void setEnt11_str01(String str);

    public String getEnt11_str02();

    public void setEnt11_str02(String str);

    public String getEnt11_str03();

    public void setEnt11_str03(String str);

    public List<Embeddable11> getEnt11_list();

    public void setEnt11_list(List<Embeddable11> list);

    public LinkedList<Embeddable11> getEnt11_llist();

    public void setEnt11_llist(LinkedList<Embeddable11> llist);

    public Map<Timestamp, Embeddable11> getEnt11_map();

    public void setEnt11_map(Map<Timestamp, Embeddable11> map);

    public Set<Embeddable11> getEnt11_set();

    public void setEnt11_set(Set<Embeddable11> set);

    public Vector<Embeddable11> getEnt11_vector();

    public void setEnt11_vector(Vector<Embeddable11> vector);
}
