/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan.samples;

import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct_WAR;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.ws.annocache.test.utils.TestLocalization;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;

public class AppDeployBench_HungryVehicle_war_Data {
    public static final String EAR_NAME = "AppDeployBench.ear";
    public static final String EAR_SIMPLE_NAME = "AppDeployBench";

    public static final String WAR_NAME = "HungryVehicle.war";
    public static final String WAR_SIMPLE_NAME = "HungryVehicle";

    //

    public static ClassSourceImpl_Specification_Direct_WAR createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        ClassSourceImpl_Specification_Direct_WAR warSpecification =
            factory.newWARDirectSpecification(EAR_SIMPLE_NAME, WAR_SIMPLE_NAME, Test_Base.JAVAEE_MOD_CATEGORY_NAME);

        String earPath = TestLocalization.putIntoData(EAR_NAME) + '/';

        String warPath = TestLocalization.putInto(earPath, WAR_NAME) + '/';
        warSpecification.setModulePath(warPath);

        warSpecification.setRootClassLoader( AppDeployBench_HungryVehicle_war_Data.class.getClassLoader() );

        return warSpecification;
    }
}
