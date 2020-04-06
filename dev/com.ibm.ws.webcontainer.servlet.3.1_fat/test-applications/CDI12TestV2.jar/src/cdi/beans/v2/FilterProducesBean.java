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
package cdi.beans.v2;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

/**
 * CDI Testing: Filter produces bean.
 */
@SessionScoped
public class FilterProducesBean extends CDIProducesBean implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    //

    /**
     * Override: The filter produces bean is session scoped.
     * 
     * This default implementation always answers {@link CDICaseScope#Session}.
     * 
     * @return The scope of this bean.
     */
    @Override
    public CDICaseScope getScope() {
        return CDICaseScope.Session;
    }

    //

    @Produces
    @FilterProducesType
    String getProducesText() {
        return super.basicGetProducesText();
    }

    public void doDispose(@Disposes @FilterProducesType String producesText) {
        super.basicDispose(producesText);
    }
}
