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
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import multiple.ham.common.qualifiers.User;

/**
 * JAX-RS Application class with one in-built BasicHAM with qualifier
 */
@BasicAuthenticationMechanismDefinition(realmName = "user-realm", qualifiers = { User.class })
@ApplicationPath("/")
public class SingleHAMQualifierApplication extends Application {
}