/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.basic;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public class HelloWorldResource3Child extends HelloWorldResource3 {

    private @Inject
    SimpleBean simpleBean;

    @Context
    private UriInfo uriinfo;

    Person person;

    private final String type = "PerRequest";

    public HelloWorldResource3Child() {
        this.setType(type);
    }

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println(type + " Injection successful...");
    }

    @Override
    public String getAbstractMessage() {
        String result = "Hello World";
        return type + " Resource: " + result;
    }

    @Override
    public String getAbstractUriinfo() {
        String result = "";
        result = uriinfo == null ? "null uriinfo"
                        : uriinfo.getPath();
        return type + " Resource Context: " + result;
    }

    @Override
    public String getAbstractSimpleBeanMessage() {
        String result = "";
        if (simpleBean != null)
            result = simpleBean.getMessage();
        else
            result = "simpleBean is null";

        return type + " Resource Inject: " + result;
    }

    @Override
    public String getAbstractPerson() {
        String result = "";

        if (person != null)
            result = person.talk();
        else
            result = "person is null";

        return type + " Resource Inject: " + result;
    }

    @Override
    public String getAbstractJordanException(String msgId)
                    throws JordanException {
        String name = "jordan";
        String result = "null uriinfo";
        if (msgId.trim() == name || msgId.trim().equals(name)) {
            if (uriinfo != null) {
                result = uriinfo.getPath();
            }
            throw new JordanException("JordanException: Jordan is superman, you cannot be in this url: " + result);
        }

        return type + " Resource Provider Inject: " + result;
    }
}