/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                Config20Tests.class, //LITE
                Config20ExceptionTests.class, //LITE
                Config20PropertyExpressionExceptionTests1.class, //LITE
                Config20PropertyExpressionExceptionTests2.class, //LITE
                Config20NoCDITests.class //LITE
})
public class FATSuite {

}
