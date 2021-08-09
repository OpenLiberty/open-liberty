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

import com.ibm.ws.bnd.metatype.annotation.Ext;

@ObjectClassDefinition(factoryPid = "com.ibm.ws.security.authentication.internal.jaas.jaasLoginContextEntry",
                name = "%jaasLoginContextEntry",
                description = "%jaasLoginContextEntry.desc",
                localization = "OSGI-INF/l10n/metatype")
@Ext.Alias("jaasLoginContextEntry")
@interface EntryConfig {
    @AttributeDefinition(name = "%entryName", description = "%entryName.desc")
    String name();

    @AttributeDefinition(name = "%loginModuleRef", description = "%loginModuleRef.desc", defaultValue = { "hashtable", "userNameAndPassword", "certificate", "token" })
    @Ext.ReferencePid("com.ibm.ws.security.authentication.internal.jaas.jaasLoginModuleConfig")
    String[] loginModuleRef();

    @AttributeDefinition(name = "internal", description = "internal use only", defaultValue = "${servicePidOrFilter(loginModuleRef)}")
    String JaasLoginModuleConfig_target();

    @AttributeDefinition(name = "internal", description = "internal use only", defaultValue = "${count(loginModuleRef)}")
    String JaasLoginModuleConfig_cardinality_minimum();

    @AttributeDefinition(name = "internal", description = "internal use only")
    String id();

}
