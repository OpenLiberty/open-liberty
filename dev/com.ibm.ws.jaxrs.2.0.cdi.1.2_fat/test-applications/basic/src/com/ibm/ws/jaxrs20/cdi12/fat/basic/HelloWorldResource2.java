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

import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("/helloworld2")
public class HelloWorldResource2 {

    /**
     * A static variable to hold a message. Note that for this sample, the field
     * is static because a new <code>HelloWorldResource</code> object is created
     * per request.
     */
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String tYPE) {
        this.type = tYPE;
    }

    private @Inject
    SimpleBean simpleBean;

    @Context
    private UriInfo uriinfo;

    Person person;

    UriInfo uriinfoForC;
    Person personForC;

    @Inject
    public void setPerson(Person person) {
        this.person = person;
        System.out.println(type + " Injection successful...");
    }

    public HelloWorldResource2(String type, UriInfo u, Person p) {
        this.type = type;
        this.uriinfoForC = u;
        this.personForC = p;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage() {
        String result = "Hello World";
        return type + " Resource: " + result;
    }

    @GET
    @Path("/uriinfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUriinfo() {
        String result = "";
        result = uriinfo == null ? "null uriinfo"
                        : uriinfo.getPath();
        return type + " Resource Context: " + result;
    }

    @GET
    @Path("/simplebean")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSimpleBean() {
        String result = "";
        if (simpleBean != null)
            result = simpleBean.getMessage();
        else
            result = "simpleBean is null";

        return type + " Resource Inject: " + result;
    }

    @GET
    @Path("/person")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPerson() {
        String result = "";

        if (person != null)
            result = person.talk();
        else
            result = "person is null";

        return type + " Resource Inject: " + result;
    }

    @GET
    @Path("/contextfromapp")
    @Produces(MediaType.TEXT_PLAIN)
    public String getContextFromAppC() {
        String result = "";
        result = uriinfoForC == null ? "null uriinfo"
                        : uriinfoForC.getPath();
        return type + " Resource Application Context: " + result;
    }

    @GET
    @Path("/personfromapp")
    @Produces(MediaType.TEXT_PLAIN)
    public String getPersonFromAppC() {
        String result = "";

        if (personForC != null)
            result = personForC.talk();
        else
            result = "personForC is null";

        return type + " Resource Application Inject: " + result;
    }

    @GET
    @Path("/provider/{id}")
    public String getJordanException(@PathParam("id") String msgId)
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

    @GET
    @Path("/cdibeans")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMessage2() throws NamingException {

        InitialContext ic = new InitialContext();
        BeanManager bm = (BeanManager) ic.lookup("java:comp/BeanManager");

        Set<Bean<?>> allBeans = bm.getBeans(Object.class,
                                            new AnnotationLiteral<Any>() {});
        String result = "";
        Iterator<Bean<?>> i = allBeans.iterator();

        while (i.hasNext()) {
            Bean<?> bean = i.next();
            String anno = "";
            for (int j = 0; j < bean.getBeanClass().getAnnotations().length; j++) {
                anno += bean.getBeanClass().getAnnotations()[j].annotationType().getName() + "|";
            }
            result += "bean.getBeanClass(): " + bean.getBeanClass() + "\n"
                      + "bean.getTypes(): " + bean.getTypes().toString() + "\n"
                      + "bean.getScope(): " + bean.getScope().toString() + "\n"
                      + "bean.getInjectionPoints(): " + bean.getInjectionPoints().toString() + "\n"
                      + "bean.getBeanClass().getAnnotations(): " + anno + "\n"
                      + "bean.toStirng():\n" + bean.toString() + ", <br>\n\n\n\n";
        }
        return result;
    }
}