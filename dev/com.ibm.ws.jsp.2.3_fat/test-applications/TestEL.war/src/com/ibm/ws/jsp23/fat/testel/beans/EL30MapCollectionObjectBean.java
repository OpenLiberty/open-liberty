/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.testel.beans;

import java.util.Collection;
import java.util.Map;

/**
 * Simple bean used for Map Collection Object
 */
public class EL30MapCollectionObjectBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Integer, String> map;

    public EL30MapCollectionObjectBean() {
        map = null;
    }

    public void setMap(Map<Integer, String> m) {
        map = m;
    }

    public Collection<String> getMap() {
        return map.values();
    }
}
