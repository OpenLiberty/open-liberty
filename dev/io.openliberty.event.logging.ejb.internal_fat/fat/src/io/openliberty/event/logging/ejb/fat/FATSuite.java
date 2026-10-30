/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.event.logging.ejb.fat.tests.EJBEventLoggingTest;

/**
 * EJB event-logging FAT suite.
 *
 * Verifies that EJB method invocations produce correct BEGIN/END event log
 * entries for all four event types (async, mdb, remote, timer) across three
 * packaging scenarios (EJB-JAR-in-EAR, standalone WAR, standalone EJB JAR).
 */
@RunWith(Suite.class)
@SuiteClasses({
                EJBEventLoggingTest.class
})
public class FATSuite {
}
