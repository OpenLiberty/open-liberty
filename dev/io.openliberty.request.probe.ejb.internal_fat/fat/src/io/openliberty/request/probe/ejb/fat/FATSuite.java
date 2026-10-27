/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.request.probe.ejb.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.request.probe.ejb.fat.tests.RequestProbeEJBRegTDTest;

/**
 * EJB request-probe FAT suite.
 *
 * EE-level repeat actions are intentionally omitted. The transform descriptor
 * registration logic tested here is fixed in product code and does not vary across
 * Jakarta EE levels. Instead, testAllFourTDsRegisterWithEachEJBFeature exercises
 * every EJB feature name — both the EE7/EE8 names (ejb-3.2, mdb-3.2, …) and the
 * EE9+ names (enterpriseBeans-4.0, mdb-4.0, …) — in a single server run, giving
 * complete coverage without the overhead of five server restarts.
 *
 * This matches the precedent set by com.ibm.ws.request.probe.servlet_fat.
 */
@RunWith(Suite.class)
@SuiteClasses({
                RequestProbeEJBRegTDTest.class
})
public class FATSuite {
}
