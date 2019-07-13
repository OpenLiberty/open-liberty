/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.utils;

import java.util.List;

public class InvalidArgumentValueException extends IllegalArgumentException {

    private String argName;
    private String invalidValue;
    private List<String> permittedValues;
    
    public InvalidArgumentValueException(String argName, String invalidValue, List<String> permittedValues) {
        
        super( "Invalid argument: " + argName + "=" + invalidValue + ". Permitted values: " + String.valueOf(permittedValues) );
        
        this.argName = argName;
        this.invalidValue = invalidValue;
        this.permittedValues = permittedValues;
    }

    public String getArgName() {
        return argName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

    public List<String> getPermittedValues() {
        return permittedValues;
    }
}
