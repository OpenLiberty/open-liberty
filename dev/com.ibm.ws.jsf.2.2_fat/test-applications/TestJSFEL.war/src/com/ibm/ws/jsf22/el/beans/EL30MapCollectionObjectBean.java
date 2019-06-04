/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.el.beans;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Simple bean used for Map Collection Object
 */

@ManagedBean(name = "mapbean")
@SessionScoped
public class EL30MapCollectionObjectBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Integer, String> map;

    public EL30MapCollectionObjectBean() {
        map = new HashMap<Integer, String>();
    }

    @PostConstruct
    protected void init() {
        map.put(1, "1");
        map.put(2, "4");
        map.put(3, "3");
        map.put(4, "2");
        map.put(5, "5");
        map.put(6, "3");
        map.put(7, "1");
    }

    public void setMap(Map<Integer, String> m) {
        map = m;
    }

    public Collection<String> getMap() {
        return map.values();
    }
}
