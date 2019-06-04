/*
 * Copyright (c) 2015, 2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.html5;

import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 * The test bean for the HTML 5 tests for JSF 2.2
 */
@ManagedBean(name = "html5TestBean")
@ApplicationScoped
public class html5TestBean {

    public String getInputTestValue() {
        return "TestValue";
    }

    public String getpassThroughTestValue() {
        return "PassThroughTestValue";
    }

    public Map<String, Object> getTestPassthroughAttributesList() {
        Map<String, Object> obj = new HashMap<String, Object>();

        obj.put("placeholder", "TestData");
        obj.put("type", "email");
        obj.put("data-test", "DataTested");

        return obj;
    }

    public String getPlaceholderTestText() {
        return "PlaceHolderTextTest";
    }
}
