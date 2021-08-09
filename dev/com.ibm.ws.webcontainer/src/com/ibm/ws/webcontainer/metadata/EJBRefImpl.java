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
package com.ibm.ws.webcontainer.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.EJBRef;

/**
 *
 */
public class EJBRefImpl extends AbstractResourceGroup implements EJBRef {

    private List<Description> descriptions;

    private String home;

    private String ejbInterface;

    private int kind;

    private String link;

    private int type;

    public EJBRefImpl(EJBRef ejbRef) {
        super(ejbRef);
        this.descriptions = new ArrayList<Description>(ejbRef.getDescriptions());
        this.home = ejbRef.getHome();
        this.ejbInterface = ejbRef.getInterface();
        this.kind = ejbRef.getKindValue();
        this.link = ejbRef.getLink();
        this.type = ejbRef.getTypeValue();
    }

    /** {@inheritDoc} */
    @Override
    public List<Description> getDescriptions() {
        return Collections.unmodifiableList(descriptions);
    }

    /** {@inheritDoc} */
    @Override
    public String getHome() {
        return home;
    }

    /** {@inheritDoc} */
    @Override
    public String getInterface() {
        return ejbInterface;
    }

    /** {@inheritDoc} */
    @Override
    public int getKindValue() {
        return kind;
    }

    /** {@inheritDoc} */
    @Override
    public String getLink() {
        return link;
    }

    /** {@inheritDoc} */
    @Override
    public int getTypeValue() {
        return type;
    }
}
