/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty 20.0.0.6
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.common;

import java.util.Collections;
import java.util.List;

import javax.websocket.Extension;

/**
 *
 */
public class ExtensionExt implements Extension {

    private String name = "";

    private List<Parameter> parameters = Collections.emptyList();

    public ExtensionExt(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.Extension#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.Extension#getParameters()
     */
    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

}
