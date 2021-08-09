/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
