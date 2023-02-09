/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

import java.io.Serializable;
import java.util.Map;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.faces.annotation.ViewMap;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named(value = "viewScopedBean")
@ViewScoped
@SuppressWarnings("serial")
public class ViewScopedBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * ViewMap should contain view data for this @ViewScoped bean
     *
     * @return the viewMap
     */
    public String getViewMap() {
        Map<String, Object> viewMap = CDI.current().select(new TypeLiteral<Map<String, Object>>() {}, ViewMap.Literal.INSTANCE).get();
        return Boolean.toString(!viewMap.isEmpty() && viewMap.toString().contains(this.getClass().getCanonicalName()));
    }

}
