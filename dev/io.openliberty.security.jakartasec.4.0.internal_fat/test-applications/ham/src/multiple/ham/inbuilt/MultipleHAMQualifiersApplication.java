/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package multiple.ham.inbuilt;

import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.LoginToContinue;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import multiple.ham.common.qualifiers.Admin;
import multiple.ham.common.qualifiers.Operator;
import multiple.ham.common.qualifiers.Tester;
import multiple.ham.common.qualifiers.User;

/**
 * JAX-RS Application class with three in-built HAMs with qualifiers; Form, CustomForm and Basic
 * This is used for testing built-in HAMs with qualifiers.
 */
@BasicAuthenticationMechanismDefinition(realmName = "admin-realm", qualifiers = { Admin.class })
@BasicAuthenticationMechanismDefinition(realmName = "user-realm", qualifiers = { User.class })
@CustomFormAuthenticationMechanismDefinition(
                                             loginToContinue = @LoginToContinue(errorPage = "/operator-login-error.html",
                                                                                loginPage = "/operator-login.html"),
                                             qualifiers = { Operator.class })
@FormAuthenticationMechanismDefinition(
                                       loginToContinue = @LoginToContinue(errorPage = "/login-error.html",
                                                                          loginPage = "/login.html"),
                                       qualifiers = { Tester.class })
@ApplicationPath("/")
public class MultipleHAMQualifiersApplication extends Application {
}