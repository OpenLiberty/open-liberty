/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.builtinAnno;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
@Named("CHEESECAKE")
public class Cheesecake implements Cake {
    @Inject
    private SomeRandomClass someRandom;

    @Override
    public String greeting() {
        return "Hello Builtin Application Scoped Bean named CHEESECAKE!";
    }

    @Override
    public Bean<?> getCakeBean() {
        return someRandom.getMyBean();
    }

    /*
     * (non-Javadoc)
     *
     * @see builtinAnnoApp.web.Cake#getIngredients()
     */
    @Override
    public CakeIngredients getIngredients() {
        // TODO Auto-generated method stub
        return null;
    }

}
