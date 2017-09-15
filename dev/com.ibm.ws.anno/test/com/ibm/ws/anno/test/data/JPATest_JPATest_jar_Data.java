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

package com.ibm.ws.anno.test.data;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Direct_Bundle;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

public class JPATest_JPATest_jar_Data {
    public static final String APP_NAME = "JPATest.app_1.0.0.201111251517.eba.unpacked";
    public static final String EBAJAR_NAME = "JPATest_1.0.0.201111251517.jar";

    public static ClassSource_Specification_Direct_Bundle createClassSourceSpecification(ClassSource_Factory classSourceFactory,
                                                                                         String projectPath,
                                                                                         String dataPath) {
        ClassSource_Specification_Direct_Bundle ebaSpecification = classSourceFactory.newEBASpecification();

        ebaSpecification.setImmediatePath(Common_Data.putIntoPath(projectPath, dataPath, EBAJAR_NAME));

        return ebaSpecification;
    }
}
