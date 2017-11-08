/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package builtinAnnoApp.web;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import javax.inject.Named;

@ApplicationScoped
//We will programmatically add a @Typed annotation, equivalent to the following, through the CakeExtension
//@Typed(Cake.class)
@Named("LEMONDRIZZLE")
public class Lemondrizzle implements Cake {
    @Inject
    private SomeRandomClass someRandom;

    @Override
    public String greeting() {
        return "Hello Builtin Application Scoped Bean named LEMONDRIZZLE!";
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
