/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package bval.v20.customprovider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/CustomProviderTestServlet")
public class CustomProviderTestServlet extends FATServlet {

    // Do some sort of CDI injection to ensure the module is CDI enabled
    @Inject
    BasicCDIBean bean;

    /**
     * Test that a custom ValidatorFactory can be used if specified in validation.xml
     * In this case, no CDI extension is provided with the ValidationProvider so the provider
     * cannot be injected using CDI.
     */
    @Test
    public void testCustomProvider() throws Exception {
        assertNotNull(bean);

        ValidationProviderResolver resolver = new ValidationProviderResolver() {

            @Override
            public List<ValidationProvider<?>> getValidationProviders() {
                List<ValidationProvider<?>> list = new ArrayList<ValidationProvider<?>>();
                list.add(new MyCustomBvalProvider());
                return list;
            }
        };

        ValidatorFactory vf = Validation.byDefaultProvider()
                        .providerResolver(resolver)
                        .configure()
                        .buildValidatorFactory();

        System.out.println("Got custom ValidatorFactory: " + vf);
        assertNotNull(vf);
        assertTrue("Expected instanceof MyCustomValidatorFactory but was: " + vf,
                   vf instanceof MyCustomValidatorFactory);
    }

}
