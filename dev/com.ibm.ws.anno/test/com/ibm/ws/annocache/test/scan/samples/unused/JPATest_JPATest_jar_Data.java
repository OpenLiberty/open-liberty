/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.samples.unused;

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_Bundle;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class JPATest_JPATest_jar_Data {
    public static final String EBA_NAME = "JPATest.app_1.0.0.201111251517.eba";
    public static final String EBA_SIMPLE_NAME = "JPATest";

    public static final String EBAJAR_NAME = "JPATest_1.0.0.201111251517.jar";
    public static final String EBAJAR_SIMPLE_NAME = "JPATest";

    public static ClassSourceImpl_Specification_Direct_Bundle createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        ClassSourceImpl_Specification_Direct_Bundle ebaSpecification =
            factory.newBundleDirectSpecification(EBA_SIMPLE_NAME, EBAJAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        ebaSpecification.setModulePath(TestLocalization.putIntoData(EBA_NAME + '/', EBAJAR_NAME));

        ebaSpecification.setRootClassLoader(JPATest_JPATest_jar_Data.class.getClassLoader());

        return ebaSpecification;
    }
}
