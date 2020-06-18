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
package com.ibm.ws.jaxrs.fat.annotation.multipleapp;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("app8")
public class App8 extends Application {

    @Override
    public Map<String,Object> getProperties() {
        Map<String,Object> properties = new HashMap<String,Object>();
        properties.put("TestProperty", 100);
        return properties;
    }

    /*
     * returns only a subset of resource classes
     */
}
