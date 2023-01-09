/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testsci.jar.servlets;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;


import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;

import jakarta.servlet.annotation.HandlesTypes;

/*
 * SCI set SessionCookieConfig which application/servlet will retrieve at runtime to do what they wish with these cookie values.
 */
@HandlesTypes(jakarta.servlet.Servlet.class)
public class ServletContainerInitializerImpl implements ServletContainerInitializer {
    private static final String CLASS_NAME = ServletContainerInitializerImpl.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public static final boolean isSetCookieConfigViaSCI;

    //set jvm.options set.cookie.config.sci.setCookieConfigViaSCI=false to disable SessionCookieConfig via SCI
    static {
        boolean setCookieConfigViaSCI = ((Boolean)AccessController.<Boolean>doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                return Boolean.valueOf(System.getProperty("set.cookie.config.sci.setCookieConfigViaSCI", "true"));
            }
        })).booleanValue();

        if (setCookieConfigViaSCI) {
            isSetCookieConfigViaSCI = true;
        } 
        else {
            isSetCookieConfigViaSCI = false;
        } 
    }

    @Override
    public void onStartup(Set<Class<?>> setOfClassesInterestedIn, ServletContext context) throws ServletException {

        LOG.info("ServletContainerInitializerImpl.onStartup ENTER");

        //add a servlet programmatically
        ServletRegistration.Dynamic dynamic = context.addServlet("TestSessionCookieConfigServlet_addedBy_SCI", testsci.jar.servlets.TestSessionCookieConfigServlet.class);
        dynamic.addMapping("/TestSessionCookieConfigServlet");

        LOG.info("JVM property set.cookie.config.sci.setCookieConfigViaSCI set to [" + isSetCookieConfigViaSCI + "]");

        //This will override the web.xml setting
        if (isSetCookieConfigViaSCI){
            addSessionCookieConfig(context);
        }

        LOG.info("ServletContainerInitializerImpl.onStartup RETURN");
    }

    private void addSessionCookieConfig(ServletContext sc) {
        LOG.info("ENTER addSessionCookieConfig from context -> " + sc);

        SessionCookieConfig scc = sc.getSessionCookieConfig();
        LOG.info(" getSessionCookieConfig returns -> " +  scc );

        scc.setDomain("setDomain_viaSCI");
        scc.setMaxAge(2022);
        scc.setName("SessionCookieConfig6_viaSCI");
        scc.setPath("setPATH_viaSCI");
        scc.setHttpOnly(true);
        scc.setSecure(true);

        //Servlet 6.0 new API
        scc.setAttribute("AttName1", "AttValue1SCI");      //overwrite web.xml setting - precendence test
        scc.setAttribute("AttName2", "AttValue2SCI");      //overwrite web.xml setting - precendence test
        scc.setAttribute("AttName5", "AttToBeReplaced");
        scc.setAttribute("AttName5", "AttValue5");         //replace attribute 
        
        //Set specific attributes using setAttribute()
        scc.setAttribute("SameSite", "None");        
        scc.setAttribute("Domain", "setAttDomain_viaSCI");        
        
        //Test null and invalid  attribute names
        try {
            scc.setAttribute(null,"NameIsNull");
            scc.setAttribute("ReportedNullAttName", "FAIL");
        }
        catch (Exception e) {
            LOG.info(" scc.setAtttribute null name.  Expecting IllegalArgumentException.  actual exception [" + e + "]");
            if (e.getMessage().contains("SESN8600E")) {                 // translated message contains prefix+code
                scc.setAttribute("ReportedNullAttName", "PASS");
            }
            else
                scc.setAttribute("ReportedNullAttName", "FAIL");
        }
        
        try {
            scc.setAttribute("Name?Invalid","NameHasQuestionMark");
            scc.setAttribute("ReportedInvalidAttName", "FAIL");
        }
        catch (Exception e) {
            LOG.info(" scc.setAtttribute name has invalid character.  Expecting IllegalArgumentException.  actual exception [" + e + "]");
            if (e.getMessage().contains("SESN8601E")) {
                scc.setAttribute("ReportedInvalidAttName", "PASS");
            }
            else
                scc.setAttribute("ReportedInvalidAttName", "FAIL");
        }

        LOG.info("RETURN addSessionCookieConfig");
    }
}
