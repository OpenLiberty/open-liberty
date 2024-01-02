/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.testel.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import jakarta.el.ELClass;
import jakarta.el.ELProcessor;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.LambdaExpression;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.el.stream.Stream;
import org.apache.jasper.el.ELContextImpl;

/**
 * This servlet tests the behavior/implementation and order of the StaticFieldELResolver
 * and StreamELResolver from CompositeELResolver.
 * 
 * Copied from JSP 2.3 FAT
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
                add("class jakarta.el.StaticFieldELResolver");
                add("class jakarta.el.MapELResolver");
                add("class jakarta.el.ResourceBundleELResolver");
                add("class jakarta.el.ListELResolver");
                add("class jakarta.el.ArrayELResolver");
                add("class jakarta.el.RecordELResolver"); // New in Pages 4.0 / Expression Language 5.0
                add("class jakarta.el.BeanELResolver");
            }
        };
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // ExpressionFactory factory = ExpressionFactory.newInstance();
        // Pages 4.0 Change: ELContextImpl & getDefaultResolver no longer use ExpressionFactory as an arguement
        ELContextImpl context = new ELContextImpl();
        PrintWriter pw = response.getWriter();

        ELResolver elResolver = ELContextImpl.getDefaultResolver(); // Removed arg for Pages 4.0

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
