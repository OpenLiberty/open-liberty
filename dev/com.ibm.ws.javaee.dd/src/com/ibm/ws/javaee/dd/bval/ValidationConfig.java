/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.bval;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

public interface ValidationConfig extends DeploymentDescriptor {

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
