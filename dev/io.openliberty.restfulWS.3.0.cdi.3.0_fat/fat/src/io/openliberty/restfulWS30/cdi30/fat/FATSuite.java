/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.restfulWS30.cdi30.fat.test.ApplicationSingletonsTest;
import io.openliberty.restfulWS30.cdi30.fat.test.Basic12Test;
import io.openliberty.restfulWS30.cdi30.fat.test.Complex12Test;
import io.openliberty.restfulWS30.cdi30.fat.test.DisableTest;
import io.openliberty.restfulWS30.cdi30.fat.test.LifeCycle12Test;
import io.openliberty.restfulWS30.cdi30.fat.test.LifeCycleMismatch12Test;

@RunWith(Suite.class)
@SuiteClasses({
               AlwaysPassesTest.class,
               ApplicationSingletonsTest.class,
               Basic12Test.class,
               Complex12Test.class,
               DisableTest.class,
               LifeCycle12Test.class,
               LifeCycleMismatch12Test.class
})
public class FATSuite {}
