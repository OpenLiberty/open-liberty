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
package test.config.extensions.schema.generator;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.metatype.SchemaGenerator;
import com.ibm.websphere.metatype.SchemaGeneratorOptions;

public class GeneratorTest {

    private SchemaGenerator schemaGenerator = null;

    public void activate(ComponentContext compContext) {

        BundleContext bundleContext = compContext.getBundleContext();
        List<Bundle> metatypeBundles = new ArrayList<Bundle>();
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().startsWith("test.config.extensions"))
                metatypeBundles.add(bundle);
        }

        SchemaGeneratorOptions options = new SchemaGeneratorOptions();
        options.setEncoding("UTF-8");
        options.setBundles(metatypeBundles.toArray(new Bundle[] {}));

        // generate schema. We don't do anything with the generated schema, but use this to generate
        // the error messages that 
        try {
            if (schemaGenerator != null)
                schemaGenerator.generate(new ByteArrayOutputStream(), options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deactivate(ComponentContext context) {
        //NOP
    }

    public void setSchemaGenerator(SchemaGenerator generator) {
        this.schemaGenerator = generator;
    }
}
