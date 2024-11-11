/*******************************************************************************
 * Copyright (c) 2017,2024 IBM Corporation and others.
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
package com.ibm.ws.jndi.iiop;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import javax.naming.spi.ObjectFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.ibm.ws.bnd.metatype.annotation.Ext;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;

@ObjectClassDefinition(name = "internal", description = "internal use only", localization = "OSGI-INF/l10n/metatype")
@Ext.ObjectClassClass(ApplicationPrereq.class)
@interface CorbanameUrlContextFactoryOcd {
    @AttributeDefinition(name = "internal", description = "internal use only")
    String id();
    @AttributeDefinition(name = "internal", description = "internal use only", defaultValue = "${id}")
    @Ext.Unique(ApplicationPrereq.APP_PREREQ_ID)
    @Ext.Final
    String application_prereq_id();
}

@Component(configurationPolicy = REQUIRE, property = "osgi.jndi.url.scheme=corbaname")
@Designate(ocd = CorbanameUrlContextFactoryOcd.class)
public class CorbanameUrlContextFactory extends UrlContextFactory implements ObjectFactory, ApplicationRecycleComponent, ApplicationPrereq {
    @Activate
    public CorbanameUrlContextFactory(@Reference ClientORBRef orbRef) {
        super(orbRef);
    }
}
