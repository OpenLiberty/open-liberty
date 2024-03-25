/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi.ui.internal.fat.app;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 *
 */
@OpenAPIDefinition(info = @Info(title = "Generated API", version = "1.0",
                                contact = @Contact(name = "Test Author",
                                                   email = "tauthor@example.com",
                                                   url = "http://example.com/tauthor"),
                                license = @License(name = "Example License", url = "http://example.com/exampleLicense"),
                                termsOfService = "http://example.com/tos"))
@ApplicationPath("/")
public class TestApplication extends Application {

}
