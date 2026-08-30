/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    UpdatingAppClassesFatTest.class,
    UpdatingAppClassesViaMBeanFatTest.class,
    SpringLoaderTest.class,
    LibraryChangeListenerTest.class,
    ClassloaderDelegationStubGenFatTest.class,
    ForbiddenClassAccessTest.class,
    LibrarySPIFatTest.class,
    SharedLibFatTest.class,
    SharedLibFatFolderTest.class,
    CommonLibFatTest.class,
    ClassProviderFatTest.class,
    ApiTypeVisibilityLibraryFatTest.class,
    ClassloaderDelegationFatTest.class,
    LibraryExporterFatTest.class
})
public class FATSuite {}
