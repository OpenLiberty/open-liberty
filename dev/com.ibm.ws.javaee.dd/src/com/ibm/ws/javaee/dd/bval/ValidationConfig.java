/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.bval;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

public interface ValidationConfig extends DeploymentDescriptor {
    String DD_SHORT_NAME = "validation.xml";

    String DD_NAME_WEB = "WEB-INF/validation.xml";
    String DD_NAME_EJB = "META-INF/validation.xml";
    String DD_NAME_WEB_ALT = "WEB-INF/classes/META-INF/validation.xml";

    String[] DD_NAMES = {
        DD_NAME_WEB,
        DD_NAME_WEB_ALT,
        DD_NAME_EJB
    };
    
    int VERSION_1_0 = 10;
    int VERSION_1_1 = 11;
    
    // The "1.0" validation configuration schema does not have
    // a version attribute.
    // String VERSION_1_0_STR = "1.0";

    String VERSION_1_1_STR = "1.1";

    int getVersionID();

    String getDefaultProvider();

    String getMessageInterpolator();

    String getTraversableResolver();

    String getConstraintValidatorFactory();

    String getParameterNameProvider();

    ExecutableValidation getExecutableValidation();

    List<String> getConstraintMappings();

    List<Property> getProperties();
}
