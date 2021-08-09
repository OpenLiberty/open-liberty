/*
 * Copyright (c)  2015, 2019  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.viewscopedcdi;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 * A bean that is used to increment and decrement a value.
 * 
 * This bean acts both as a listener as well as a regular bean.
 * 
 * 
 * There is also a method to cause an error.
 * 
 * @author Paul Nicolucci
 */
@Named(value = "ViewScopedCDIBean")
@ViewScoped
public class ViewScopedCDIBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer value = 0;
    private static Logger LOGGER = Logger.getLogger(ViewScopedCDIBean.class.getSimpleName());

    /**
     * Return the value of the counter.
     * 
     * @return The value
     */
    public Integer getValue() {
        return this.value;
    }

    /**
     * Decrement the counter.
     */
    public void decrement() {
        this.value -= 1;
    }

    /**
     * Increment the counter.
     */
    public void increment() {
        this.value += 1;
    }

    /**
     * Increment the value of the counter.
     * 
     * @param e
     */
    public void increment(ActionEvent e) {
        this.value += 1;
    }

    /**
     * Decrement the value of the counter.
     * 
     * @param e
     */
    public void decrement(ActionEvent e) {
        this.value -= 1;
    }

    public String reset() {
        LOGGER.info("ViewScopedCDIBean reset");
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/invalidate";
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("ViewScopedCDIBean PreDestroy Invoked");
    }

    @PostConstruct
    public void create() {
        LOGGER.info("ViewScopedCDIBean PostConstruct Invoked");
    }
}
