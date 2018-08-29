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

package com.ibm.ws.anno.test.scan.samples;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_Bundle;
import com.ibm.ws.anno.test.scan.Test_Base;
import com.ibm.ws.anno.test.utils.TestLocalization;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class JPATest_JPATest_jar_Data {
    public static final String EBA_NAME = "JPATest.app_1.0.0.201111251517.eba";
    public static final String EBA_SIMPLE_NAME = "JPATest";

    public static final String EBAJAR_NAME = "JPATest_1.0.0.201111251517.jar";
    public static final String EBAJAR_SIMPLE_NAME = "JPATest";

    public static ClassSource_Specification_Direct_Bundle createClassSourceSpecification(ClassSource_Factory factory) {
        ClassSource_Specification_Direct_Bundle ebaSpecification =
            factory.newBundleDirectSpecification(EBA_SIMPLE_NAME, EBAJAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        ebaSpecification.setModulePath(TestLocalization.putIntoData(EBA_NAME + '/', EBAJAR_NAME));

        ebaSpecification.setRootClassLoader(JPATest_JPATest_jar_Data.class.getClassLoader());

        return ebaSpecification;
    }
}
