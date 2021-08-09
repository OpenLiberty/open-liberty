/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * CDI Testing: Type for {@link Produces} beans.
 */
@ApplicationScoped
public class CDIProducesBean implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    // public CDIProducesBean() {
    //     (new Throwable("Dummy for [ " + getClass().getName() + ".CDIProducesBean() ]")).printStackTrace(System.out);
    // }

    //

    /**
     * Answer the bean name.
     *
     * This implementation always answers the simple class name of the bean.
     *
     * @return The bean name.
     */
    public String getBeanName() {
        return getClass().getSimpleName();
    }

    /**
     * Answer the scope of this bean.
     *
     * This default implementation always answers {@link CDICaseScope#Application}.
     *
     * @return The scope of this bean.
     */
    public CDICaseScope getScope() {
        return CDICaseScope.Application;
    }

    //

    /**
     * CDI produces test method (core implementation). Answer the
     * bean name plus the dispose count.
     *
     * This is not the actual produces method! Per {@linkplain "http://docs.jboss.org/cdi/api/1.2/javax/enterprise/inject/Produces.html",
     * producer methods are not inherited. Subtypes must implement the actual
     * producer method.
     *
     * @return The produces test text.
     */
    String basicGetProducesText() {
        String text = getScope().getTag() + ":" + getBeanName() + ":" + Integer.toString(disposeCount);;
        // (new Throwable("Dummy for [ " + getClass().getName() + ".basicGetProducesText ] with [ " + text + " ]")).printStackTrace(System.out);
        return text;
    }

    /** Count of the times which dispose has been called. */
    private int disposeCount;

    /**
     * Answer the dispose count.
     *
     * @return The dispose count.
     */
    public int getDisposeCount() {
        return disposeCount;
    }

    /**
     * Increment the dispose count.
     */
    void incrementDisposeCount() {
        disposeCount++;
    }

    /**
     * CDI dispose test method (core implementation). Dispose of the produces text.
     *
     * This implementation increments the dispose count. This is useful for testing
     * only if the bean is scoped wider than request.
     *
     * @param producesText The text to displos.
     */
    void basicDispose(String producesText) {
        disposeCount++;
    }
}
