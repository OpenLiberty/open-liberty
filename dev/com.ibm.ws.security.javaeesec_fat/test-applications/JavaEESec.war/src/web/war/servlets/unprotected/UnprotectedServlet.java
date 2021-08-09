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
package web.war.servlets.unprotected;

import web.jar.base.FlexibleBaseServlet;
import javax.inject.Inject;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;

@BasicAuthenticationMechanismDefinition
public class UnprotectedServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public UnprotectedServlet() {
        super("UnprotectedServlet");

        mySteps.add(new ProcessServlet30MethodStep());
        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }
}
