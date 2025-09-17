/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.cdi41.internal.fat.el.ELTest;
import io.openliberty.cdi41.internal.fat.invokers.InvokersTest;

@RunWith(Suite.class)
@SuiteClasses({
                ELTest.class,
                InvokersTest.class,
})
public class FATSuite {

}
