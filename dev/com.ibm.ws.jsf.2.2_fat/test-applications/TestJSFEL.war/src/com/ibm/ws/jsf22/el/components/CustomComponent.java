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
package com.ibm.ws.jsf22.el.components;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PreRenderComponentEvent;

import org.apache.jasper.el.ELContextImpl;

/**
 * Test case used to test these two Jiras from JSF 2.2 open source implementation
 * 1) http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1164 - This ensures the ELResolver should be of correct size an order.
 * 
 * 2) http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1043 - This test case invokes 2 new methods in JSF 2.2, ComponentSystemEvent
 * isAppropriateListener() and processListener(). isAppropriateListener() is called to ensure that correct listener type is passed in before
 * calling processListener()
 * 
 */
@FacesComponent(value = "customcomponent")
@ListenerFor(systemEventClass = PreRenderComponentEvent.class)
public class CustomComponent extends UIComponentBase implements
                ComponentSystemEventListener {

    /**
     * 
     */
    @Override
    public String getFamily() {
        return "customcomponent";
    }


    /**
     * 
     */
    @Override
    public void encodeEnd(FacesContext context) throws IOException {

        ResponseWriter responseWriter = context.getResponseWriter();
        responseWriter.endElement("div");
        responseWriter.endElement("div");
        responseWriter.write("Test2: ELResolver Order check test");
        responseWriter.endElement("div");
        checkExpressionFactoryOrder(context);

    }

    //check if ELResolver is of correct size an order.
    private void checkExpressionFactoryOrder(FacesContext context) throws IOException {
        ExpressionFactory factory = ExpressionFactory.newInstance();
        ELContext elContext = context.getELContext();

        ResponseWriter responseWriter = context.getResponseWriter();;

        ELResolver elResolver = ELContextImpl.getDefaultResolver(factory);

        // Getting all EL Resolvers from the CompositeELResolver
        responseWriter.write("ELResolver list in order:  ");
        responseWriter.startElement("div", null);
        ELResolver[] resolvers = getELResolvers(elResolver, responseWriter);
        if (checkOrderAndSize(resolvers, responseWriter)) {
            responseWriter.startElement("div", null);
            responseWriter.write("The order and number of ELResolvers from the CompositeELResolver are correct!");
            responseWriter.startElement("div", null);
        } else {
            responseWriter.startElement("div", null);
            responseWriter.write("Error: Order and number of ELResolvers are incorrect!");
            responseWriter.startElement("div", null);
        }
    }

    private ELResolver[] getELResolvers(ELResolver elResolver, ResponseWriter responseWriter) throws IOException {
        try {
            Field field = elResolver.getClass().getDeclaredField("resolvers");
            field.setAccessible(true);
            if (field.get(elResolver) instanceof ELResolver[]) {
                return (ELResolver[]) field.get(elResolver);
            }
        } catch (Exception e) {
            responseWriter.startElement("div", null);
            responseWriter.write("Exception caught: " + e.getMessage());
            responseWriter.write("An exception was thrown: " + e.toString());
        }
        return null;
    }

    /**
     * Check if the order and number of ELResolvers from CompositeELResolver
     * match with the ones specified by JSF 2.2 specifications
     * 
     * @param resolvers ELResolver[]
     * @param pw PrintWriter
     * @return true if order and number of ELResolvers are correct, false otherwise
     * @throws IOException
     */
    private boolean checkOrderAndSize(ELResolver[] resolvers, ResponseWriter responseWriter) throws IOException {

        List<String> expectedResolvers = new ArrayList<String>() {
            {
                add("class org.apache.el.stream.StreamELResolverImpl");
                add("class javax.el.StaticFieldELResolver");
                add("class javax.el.MapELResolver");
                add("class javax.el.ResourceBundleELResolver");
                add("class javax.el.ListELResolver");
                add("class javax.el.ArrayELResolver");
                add("class javax.el.BeanELResolver");
            }
        };

        int elResolversCounter = 0;
        if (resolvers.length != 0) {
            for (int i = 0; i < resolvers.length; i++) {
                if (resolvers[i] != null) {
                    responseWriter.write(resolvers[i].getClass().toString());
                    elResolversCounter++;
                    if (!resolvers[i].getClass().toString().equals(expectedResolvers.get(i))) {
                        return false;
                    }
                }
            }
            if (elResolversCounter != expectedResolvers.size()) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isListenerForSource(Object source) {
        return true;
    }

    /**
     * Implement the component listener method. This test case invokes 2 new methods in JSF 2.2, ComponentSystemEvent
     * isAppropriateListener() and processListener(). isAppropriateListener() is called to ensure that correct listener
     * type is passed in before calling processListener()
     * 
     * @param event
     */
    @Override
    public void processEvent(ComponentSystemEvent event)
                    throws AbortProcessingException {
        try {
            ResponseWriter responseWriter = FacesContext.getCurrentInstance().getResponseWriter();
            responseWriter.startElement("div", null);
            responseWriter.write("Test1: JSF 2.2 new methods test");
            responseWriter.startElement("div", null);
            responseWriter.write("Invoked JSF 2.2 new methods in ComponentSystemEvent, isAppropriateListener() and processListener()");
            responseWriter.startElement("div", null);
        } catch (IOException ioe) {
            FacesContext.getCurrentInstance().getExternalContext().log(ioe.getStackTrace() + "\n" + ioe.getMessage());
        }

    }
}
