/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.jaxb;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.jaxb.JAXBDataBinding;

import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;

/**
 * This class will register the customized JAXBDataBinding in the bus instances, which will enable JAXBContext instance pool
 */
public class JAXBContextPoolInitializer implements LibertyApplicationBusListener {

    @Override
    public void preInit(Bus bus) {
        bus.setProperty(DataBinding.class.getName(), JAXBDataBinding.class);
    }

    @Override
    public void initComplete(Bus bus) {}

    @Override
    public void preShutdown(Bus bus) {}

    @Override
    public void postShutdown(Bus bus) {}

}
