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
package com.ibm.ws.dynamic.bundle;

public class ServiceReferenceDeclaration {
    private String name;
    private String iface;
    private String target;
    private String bind;
    private String unbind;

    public ServiceReferenceDeclaration name(String name) {
        this.name = name;
        return this;
    }

    public ServiceReferenceDeclaration provides(Class<?> iface) {
        return provides(iface.getName());
    }

    public ServiceReferenceDeclaration provides(String iface) {
        this.iface = iface;
        return this;
    }

    public ServiceReferenceDeclaration target(String target) {
        this.target = target;
        return this;
    }

    public ServiceReferenceDeclaration bind(String bind) {
        this.bind = bind;
        return this;
    }

    public ServiceReferenceDeclaration unbind(String unbind) {
        this.unbind = unbind;
        return this;
    }

    @Override
    public String toString() {
        return String.format("<reference name='%s' interface='%s' target='%s' bind='%s' unbind='%s' />", name, iface, target, bind, unbind);
    }
}