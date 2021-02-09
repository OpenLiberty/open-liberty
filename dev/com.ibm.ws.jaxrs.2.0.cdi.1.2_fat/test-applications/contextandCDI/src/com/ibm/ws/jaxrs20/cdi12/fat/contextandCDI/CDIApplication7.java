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
package com.ibm.ws.jaxrs20.cdi12.fat.contextandCDI;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("contextandCDI7")
public class CDIApplication7 extends Application {

   @Inject TestResource5 resource; 
    
    @Override
    public Set<Object> getSingletons() {
       
        LinkedHashSet<Object> classes = new LinkedHashSet<>();
        classes.add(resource);
        return classes;
    }
    
    @Override
    public Map<String,Object> getProperties() {
        Map<String,Object> properties = new HashMap<String,Object>();
        properties.put("TestProperty", 100);
        return properties;
    }

}

