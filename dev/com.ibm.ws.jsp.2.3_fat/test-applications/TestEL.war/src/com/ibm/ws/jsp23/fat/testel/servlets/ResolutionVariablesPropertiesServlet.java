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
package com.ibm.ws.jsp23.fat.testel.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.el.ELClass;
import javax.el.ELProcessor;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.LambdaExpression;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.el.stream.Stream;
import org.apache.jasper.el.ELContextImpl;

/**
 * This servlet tests the behavior/implementation and order of the StaticFieldELResolver
 * and StreamELResolver from CompositeELResolver.
 */
@WebServlet("/ResolutionVariablesPropertiesServlet")
public class ResolutionVariablesPropertiesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final List<String> myList;
    private final List<String> expectedResolvers;

    /**
     * Constructor
     */
    public ResolutionVariablesPropertiesServlet() {
        super();

        myList = new ArrayList<String>() {
            {
                add("1");
                add("4");
                add("3");
                add("2");
                add("5");
                add("3");
                add("1");
            }
        };

        expectedResolvers = new ArrayList<String>() {
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
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ExpressionFactory factory = ExpressionFactory.newInstance();
        ELContextImpl context = new ELContextImpl(factory);
        PrintWriter pw = response.getWriter();

        ELResolver elResolver = ELContextImpl.getDefaultResolver(factory);

        // Getting all EL Resolvers from the CompositeELResolver
        pw.println("ELResolvers.");
        ELResolver[] resolvers = getELResolvers(elResolver, pw);
        if (checkOrderAndSize(resolvers, pw)) {
            pw.println("The order and number of ELResolvers from the CompositeELResolver are correct!");
        } else {
            pw.println("Error: Order and number of ELResolvers are incorrect!");
        }

        // Test the behavior of the new ELResolvers. That is the StaticFieldELResolver and StreamELResolver.
        pw.println("\nTesting implementation.");
        try {
            ELClass elClass = new ELClass(Boolean.class);
            pw.println("Testing StaticFieldELResolver with Boolean.TRUE (Expected: true): " + elResolver.getValue(context, elClass, "TRUE"));

            elClass = new ELClass(Integer.class);
            pw.println("Testing StaticFieldELResolver with Integer.parseInt (Expected: 86): "
                       + elResolver.invoke(context, elClass, "parseInt", new Class<?>[] { String.class, Integer.class }, new Object[] { "1010110", 2 }));

            Stream stream = (Stream) elResolver.invoke(context, myList, "stream", null, new Object[] {});
            pw.println("Testing StreamELResolver with distinct method (Expected: [1, 4, 3, 2, 5]): " + stream.distinct().toList());

            ELProcessor elp = new ELProcessor();
            LambdaExpression expression = (LambdaExpression) elp.eval("e->e>2");
            stream = (Stream) elResolver.invoke(context, myList, "stream", null, new Object[] {});
            pw.println("Testing StreamELResolver with filter method (Expected: [4, 3, 5, 3]): " + stream.filter(expression).toList());

        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("Test Failed. An exception was thrown: " + e.toString());
        }

    }

    /**
     * Get the EL Resolvers from the CompositeELResolver
     *
     * @param elResolver the EL resolver
     * @param pw PrintWriter
     * @return the array of ELResolvers
     */
    private ELResolver[] getELResolvers(ELResolver elResolver, PrintWriter pw) {
        try {
            Field field = elResolver.getClass().getDeclaredField("resolvers");
            field.setAccessible(true);
            if (field.get(elResolver) instanceof ELResolver[]) {
                return (ELResolver[]) field.get(elResolver);
            }
        } catch (Exception e) {
            pw.println("Exception caught: " + e.getMessage());
            pw.println("An exception was thrown: " + e.toString());
        }
        return null;
    }

    /**
     * Check if the order and number of ELResolvers from CompositeELResolver
     * match with the ones specified by JSP 2.3 specifications
     *
     * @param resolvers ELResolver[]
     * @param pw PrintWriter
     * @return true if order and number of ELResolvers are correct, false otherwise
     */
    private boolean checkOrderAndSize(ELResolver[] resolvers, PrintWriter pw) {
        int elResolversCounter = 0;
        if (resolvers.length != 0) {
            for (int i = 0; i < resolvers.length; i++) {
                if (resolvers[i] != null) {
                    pw.println(resolvers[i].getClass().toString());
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
}
