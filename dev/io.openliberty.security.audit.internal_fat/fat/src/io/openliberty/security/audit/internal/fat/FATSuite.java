/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.audit.internal.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * FAT suite for audit generateNewSession tests.
 *
 * Covers GH issue #29751: audit-1.0 unexpectedly creates an HTTP session
 * (and returns a JSESSIONID cookie) when auditing REST endpoints that do
 * not use sessions. Setting {@code <auditSource generateNewSession="false"/>}
 * in server.xml disables this behaviour.
 */
@RunWith(Suite.class)
@SuiteClasses({
    AuditGenerateNewSessionTest.class
})
public class FATSuite {
}
