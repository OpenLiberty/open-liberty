/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.el.components;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.el.ELManager;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.faces.application.Application;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ComponentSystemEventListener;
import javax.faces.event.ListenerFor;
import javax.faces.event.PreRenderComponentEvent;

/**
 * Test case used to test these two Jiras from JSF 2.2 open source implementation
 * 1) http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1164 - This ensures the ELResolver should be of correct size an order.
 *
 * The issue in the new Jakarta Faces project is: https://github.com/jakartaee/faces/issues/1164
 *
 * 2) http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1043 - This test case invokes 2 new methods in JSF 2.2, ComponentSystemEvent
 * isAppropriateListener() and processListener(). isAppropriateListener() is called to ensure that correct listener type is passed in before
 * calling processListener()
 *
 */
@FacesComponent(value = "customcomponent")
@ListenerFor(systemEventClass = PreRenderComponentEvent.class)
public class CustomComponent extends UIComponentBase implements ComponentSystemEventListener {

    private boolean isFaces40OrLater = false;
    private boolean isFaces41OrLater = false;

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

    /*
     * Check if ELResolver is of correct size an order.
     */
    private void checkExpressionFactoryOrder(FacesContext context) throws IOException {

        ResponseWriter responseWriter = context.getResponseWriter();;

        Application app = context.getApplication();
        ELResolver elResolver = app.getELResolver();

        if (Boolean.parseBoolean(context.getExternalContext().getRequestParameterMap().get("isFaces41OrLater"))) {
            isFaces41OrLater = true;
        }

        // Getting all EL Resolvers
        responseWriter.write("ELResolver list in order:  ");
        responseWriter.startElement("div", null);
        ELResolver[] resolvers = getELResolvers(elResolver, responseWriter);

        if (checkOrderAndSize(resolvers, responseWriter)) {
            responseWriter.startElement("div", null);
            responseWriter.write("The order and number of ELResolvers are correct!");
            responseWriter.startElement("div", null);
        } else {
            responseWriter.startElement("div", null);
            responseWriter.write("Error: Order and number of ELResolvers are incorrect!");
            responseWriter.startElement("div", null);
        }
    }

    private ELResolver[] getELResolvers(ELResolver elResolver, ResponseWriter responseWriter) throws IOException {
        try {
            /*
             * For JSF 2.2/2.3 and Faces 3.0 the org.apache.myfaces.el.unified.resolver.FacesCompositeELResolver
             * is the elResolver, which extends org.apache.myfaces.el.CompositeELResolver.
             * The org.apache.myfaces.el.CompositeELResolver extends javax.el.CompositeELResolver.
             * The org.apache.myfaces.el.CompositeELResolver has a Collection<ELResolver> _elResolvers field that
             * we can use to check what ELResolvers are added.
             *
             * For Faces 4.0 or later the javax.el.CompositeELResolver is the elResolver.
             * The javax.el.CompositeELResolver has a ELResolver[] elResolvers field that we can use to check
             * what ELResolvers are added.
             */

            //This is for Faces 3.0 and earlier.
            Class<?> parent = elResolver.getClass().getSuperclass();

            Field parentField = null;
            try {
                parentField = parent.getDeclaredField("_elResolvers");

                if (parentField != null) {
                    parentField.setAccessible(true);
                    Object parentFieldObject = parentField.get(elResolver);

                    if (parentFieldObject instanceof Collection) {
                        Collection<?> elResolverCollection = (Collection<?>) parentFieldObject;
                        ELResolver[] elResolverArray = elResolverCollection.toArray(new ELResolver[elResolverCollection.size()]);
                        return elResolverArray;
                    }
                }
            } catch (NoSuchFieldException ex) {
                // Do nothing, this will happen for Faces 4.0 and later.
            }

            // This is for Faces 4.0
            Field field = null;
            try {
                field = elResolver.getClass().getDeclaredField("resolvers");
                isFaces40OrLater = true;

                if (field != null) {
                    field.setAccessible(true);
                    if (field.get(elResolver) instanceof ELResolver[]) {
                        return (ELResolver[]) field.get(elResolver);
                    }
                }
            } catch (NoSuchFieldException ex) {
                // Do nothing, this will happen for Faces/JSF features before Faces 4.0.
            }

        } catch (Exception e) {
            responseWriter.startElement("div", null);
            responseWriter.write("Exception caught: " + e.getMessage());
            responseWriter.write("An exception was thrown: " + e.toString());
        }
        return null;
    }

    /**
     * Check if the order and number of ELResolvers
     * match with the ones specified by JSF 2.2 specifications
     *
     * @param resolvers ELResolver[]
     * @param pw        PrintWriter
     * @return true if order and number of ELResolvers are correct, false otherwise
     * @throws IOException
     */
    private boolean checkOrderAndSize(ELResolver[] resolvers, ResponseWriter responseWriter) throws IOException {
        // The JSF/Faces specification says the StreamELResolver should be: The return from ExpressionFactory.getStreamELResolver()
        ExpressionFactory expFactory = ELManager.getExpressionFactory();
        List<String> expectedResolvers = new ArrayList<String>() {
            private static final long serialVersionUID = 1L;

            {

                add(expFactory.getStreamELResolver().getClass().getName());
                add("javax.el.StaticFieldELResolver");
                add("javax.el.MapELResolver");
                add("javax.el.ListELResolver");
                add("javax.el.ArrayELResolver");
                if (isFaces41OrLater) {
                    add("jakarta.el.OptionalELResolver");
                    add("jakarta.el.RecordELResolver");
                }
                if (isFaces40OrLater) {
                    add("javax.el.BeanELResolver"); // swap with add("org.apache.myfaces.el.resolver.LambdaBeanELResolver"); when o.a.m.USE_LAMBDA_METAFACTORY is true
                } else {
                    add("javax.el.BeanELResolver");
                }
            }
        };

        // Loop over all the resolvers until we find the StreamELResolver and then
        // copy the next 5 resolvers, 7 if Faces 4.1 or later into a new array to use for comparison.
        int firstResolverFoundIndex = 0;
        List<String> actualResolvers = new ArrayList<String>();
        for (int i = 0; i < resolvers.length; i++) {
            if (resolvers[i].getClass().getName().equals(expectedResolvers.get(0))) {
                firstResolverFoundIndex = i;
                break;
            }
        }

        // Add all the actual resolvers to the actualResolvers list.
        int resolverCount;
        if (isFaces41OrLater) {
            resolverCount = 7; // Added OptionalELResolver and RecordELResolver
        } else {
            resolverCount = 5;
        }

        for (int i = firstResolverFoundIndex; i <= firstResolverFoundIndex + resolverCount; i++) {
            actualResolvers.add(resolvers[i].getClass().getName());
        }

        // Compare the two lists to ensure they have the expected resolvers in the expected order.
        int elResolversCounter = 0;
        if (actualResolvers.size() != 0) {
            String resolver;
            for (int i = 0; i < actualResolvers.size(); i++) {
                resolver = actualResolvers.get(i);
                if (resolver != null) {
                    responseWriter.write(resolver);
                    elResolversCounter++;
                    if (!resolver.equals(expectedResolvers.get(i))) {
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
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
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
