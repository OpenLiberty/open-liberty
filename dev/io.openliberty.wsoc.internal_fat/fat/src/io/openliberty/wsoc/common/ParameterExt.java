/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import javax.websocket.Extension.Parameter;

/**
 *
 */
public class ParameterExt implements Parameter {

    private String name = "";
    private String value = "";

    public ParameterExt(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.Extension.Parameter#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.Extension.Parameter#getValue()
     */
    @Override
    public String getValue() {
        return value;
    }

}
