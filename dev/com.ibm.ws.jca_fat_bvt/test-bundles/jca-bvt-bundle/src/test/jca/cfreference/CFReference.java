/*******************************************************************************
 * Copyright (c) 2016,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.cfreference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.ws.bnd.metatype.annotation.Ext.Alias;
import com.ibm.ws.bnd.metatype.annotation.Ext.Service;
import com.ibm.wsspi.resource.ResourceFactory;

/**
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CFReference {

    @Reference
    protected ResourceFactory jcaConnectionFactory;

    protected void activate(Config config) {
        System.out.println("CFReference successfully bound resource factory");
    }

}

@Alias("jcaTestCFReference")
@ObjectClassDefinition
@interface Config {
    @Service("com.ibm.wsspi.resource.ResourceFactory")
    String jcaConnectionFactory();

    String jcaConnectionFactory_target() default "(service.pid=${jcaConnectionFactory})";
}
