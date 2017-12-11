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
package web.jar.mechanisms.form.get.forward;
import web.jar.base.FlexibleBaseServlet;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=true, useForwardToLoginExpression=""))
public class HAMDefinition {
}
