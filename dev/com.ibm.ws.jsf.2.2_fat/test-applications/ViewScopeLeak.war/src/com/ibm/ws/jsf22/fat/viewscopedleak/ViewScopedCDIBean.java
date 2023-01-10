/*
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.viewscopedleak;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 * CDI ViewScoped Bean. Used as part of the JSF View Lifecycle (created at the start of a view and deleted once the view changes)
 *
 * See testMyFaces4433 ( test for memory leak )
 *
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
