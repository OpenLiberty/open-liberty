/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.servlets.customform.forward;
import web.jar.base.FlexibleBaseServlet;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

@CustomFormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/customLoginError.jsp", loginPage="/customLogin.xhtml", useForwardToLogin=true, useForwardToLoginExpression=""))
@WebServlet("/MultipleISCustomFormServlet")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = {@HttpMethodConstraint(value = "GET", rolesAllowed = "grantedgroup")})
public class MultipleISCustomFormServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public MultipleISCustomFormServlet() {
        super("MultipleISCustomFormServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
        mySteps.add(new WriteJSR375Step());
    }
}
