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
package com.ibm.ws.security.jaas.common.internal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

import com.ibm.ws.bnd.metatype.annotation.Ext;

@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.authentication.internal.jaas.jaasLoginModuleConfig",
                name = "%jaasLoginModule",
                description = "%jaasLoginModule.desc",
                localization = "OSGI-INF/l10n/metatype")
@Ext.Alias("jaasLoginModule")
public @interface ModuleConfig {

    @AttributeDefinition(name = "%className", description = "%className.desc")
    String className();

    @AttributeDefinition(name = "%controlFlag", description = "%controlFlag.desc", defaultValue = "REQUIRED",
                    options = { @Option(value = "REQUIRED", label = "%controlFlag.REQUIRED"),
                               @Option(value = "REQUISITE", label = "%controlFlag.REQUISITE"),
                               @Option(value = "SUFFICIENT", label = "%controlFlag.SUFFICIENT"),
                               @Option(value = "OPTIONAL", label = "%controlFlag.OPTIONAL") })
    String controlFlag();

    @AttributeDefinition(name = "%libraryRef", description = "%libraryRef.desc")
    @Ext.ReferencePid("com.ibm.ws.classloading.sharedlibrary")
    String libraryRef();

    @AttributeDefinition(name = "internal", description = "internal use only", defaultValue = "(service.pid=${libraryRef})")
    String SharedLib_target();

    @AttributeDefinition(name = "%optionsRef", description = "%optionsRef.desc", required = false)
    @Ext.FlatReferencePid("com.ibm.ws.security.authentication.internal.jaas.jaasLoginModuleConfig.options")
    String options();

    @AttributeDefinition(name = "internal", description = "internal use only")
    String id();

}
