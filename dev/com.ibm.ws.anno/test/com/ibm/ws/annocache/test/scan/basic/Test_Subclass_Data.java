/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.basic;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_EJB;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;

public class Test_Subclass_Data {
    public static final String EAR_NAME = "Subclass.ear";
    public static final String EAR_SIMPLE_NAME = "Subclass";

    public static final String EJBJAR_NAME = "Subclass.jar";
    public static final String EJBJAR_SIMPLE_NAME = "Subclass";

    public static ClassSourceImpl_Specification_Direct_EJB
        createClassSourceSpecification(ClassSourceImpl_Factory classSourceFactory) {

        ClassSourceImpl_Specification_Direct_EJB ejbSpecification =
            classSourceFactory.newEJBDirectSpecification(EAR_SIMPLE_NAME, EJBJAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        ejbSpecification.setModulePath(TestLocalization.getClassesPath());

        ejbSpecification.setRootClassLoader(Test_AnnoBasic_Data.class.getClassLoader());

        return ejbSpecification;
    }
}
