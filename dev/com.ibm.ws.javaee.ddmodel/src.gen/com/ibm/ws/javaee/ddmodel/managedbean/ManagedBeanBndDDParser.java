/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.managedbean;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ManagedBeanBndDDParser extends DDParserBndExt {
    public ManagedBeanBndDDParser(Container ddRootContainer, Entry ddEntry) throws DDParser.ParseException {
        super(ddRootContainer, ddEntry, UNUSED_CROSS_COMPONENT_TYPE,
              !IS_XMI, "managed-bean-bnd",
              null,
              XML_VERSION_MAPPINGS_10_11, 11);
    }

    @Override
    public ManagedBeanBndType parse() throws ParseException {
        super.parseRootElement();
        return (ManagedBeanBndType) rootParsable;
    }

    @Override
    protected ManagedBeanBndType createRootParsable() throws ParseException {
        return (ManagedBeanBndType) super.createRootParsable();
    }    

    @Override
    protected ManagedBeanBndType createRoot() {
        return new ManagedBeanBndType( getDeploymentDescriptorPath() );
    }
}
