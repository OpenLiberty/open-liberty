/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch.simpleresource;

import java.io.Serializable;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@RequestScoped
@Named
public class SimpleBean implements Serializable {

	private static final long serialVersionUID = 8110539535881311409L;
	String _response;

    public String getResponse() {
        return _response;
    }

    public String getMessage() {
        _response = "Hello from SimpleBean";
        return _response;
    }

    @PreDestroy
    public void destruct() {
        System.out.println(this + " Pre Destroy called on " + this);
    }
}
