/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testsci.jar.servletsfilters;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;

//since our HandlesTypes annotation contains the class Servlet, then all classes within the
//application that implement Servlet will be sent to the onStartup method below
@HandlesTypes(javax.servlet.Servlet.class)
public class ServletContainerInitializerImpl implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> setOfClassesInterestedIn, ServletContext context) throws ServletException {

        context.log("***********< ServletContainerInitializerImpl onStartup ENTER >****************");

        //going to add a context attribute to show the set of classes that were passed in
        if (setOfClassesInterestedIn != null) {
            context.setAttribute("SET_OF_SERVLETS_IN_APP", setOfClassesInterestedIn);
        } else {
            context.setAttribute("SET_OF_SERVLETS_IN_APP", "null");
        }

        //add a Filter programmatically
        //if this jar is used as a shared library, then this filter will be applied to all requests
        context.log("***********< ServletContainerInitializerImpl context.addFilter, name (MySharedFilter), class (SharedFilter)   >****************");
        FilterRegistration.Dynamic dynamic = context.addFilter("MySharedFilter", testsci.jar.servletsfilters.SharedFilter.class);

        //Note the boolean value will determine the filter order in the chain.  true will be last/after in filter chain;
        //false will be first in filter chain. So if you want these filters to be applied before web.xml filters, then use false.
        context.log("***********< ServletContainerInitializerImpl addFilter, mapping URL pattern   >****************");
        dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        context.log("***********< ServletContainerInitializerImpl addFilter, mapping servlet filter name (TestServlet_Define_After_Filter) >****************");
        //Add servlet name mapping which is never defined FIRST to cause new message: [WARNING ] CWWWC0002W: No servlet definition is found for the servlet name
        //and also test to make sure the filter chain is continue to the next mapping.
        dynamic.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, "TestServlet_WithNoDefinition");

        //This servlet is defined later below.
        dynamic.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), true, "TestServlet_Define_After_Filter");

        //add a servlet programmatically
        context.log("***********< ServletContainerInitializerImpl context.addServlet, name (TestServlet_Define_After_Filter), class (TestServlet2)   >****************");
        ServletRegistration.Dynamic dynamicServlet = context.addServlet("TestServlet_Define_After_Filter", testsci.jar.servletsfilters.TestServlet2.class);
        dynamicServlet.addMapping("/Test2ServletFilterNameMapping");
        //add another mapping
        dynamicServlet.addMapping("/TestMap");

        context.log("***********< ServletContainerInitializerImpl onStartup EXIT >****************");
    }

}
