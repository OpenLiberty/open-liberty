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
package web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import web.FlexibleBaseServlet.ProcessAccessTokenStep;
import web.FlexibleBaseServlet.ProcessIntrospectionStep;
import web.FlexibleBaseServlet.WriteAccessTokenStep;
import web.FlexibleBaseServlet.WriteCookiesStep;
import web.FlexibleBaseServlet.WriteParametersStep;
import web.FlexibleBaseServlet.WritePrincipalStep;
import web.FlexibleBaseServlet.WritePublicCredentialsStep;
import web.FlexibleBaseServlet.WriteRequestBasicsStep;
import web.FlexibleBaseServlet.WriteRolesStep;
import web.FlexibleBaseServlet.WriteRunAsSubjectStep;
import web.FlexibleBaseServlet.WriteSubjectStep;

import com.ibm.websphere.security.auth.WSSubject;

/**
 * Form Login Servlet
 */
public class FormLoginServletE2E2 extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public FormLoginServletE2E2() {
        super("FormLoginServletE2E2");
     	mySteps.add(new WriteRequestBasicsStep());
     	mySteps.add(new WritePrincipalStep());
     	mySteps.add(new WriteRolesStep());
     	mySteps.add(new WriteSubjectStep());
     	mySteps.add(new WritePublicCredentialsStep());
     	mySteps.add(new WriteAccessTokenStep());  
     	mySteps.add(new ProcessIntrospectionStep()); 

    }

}
