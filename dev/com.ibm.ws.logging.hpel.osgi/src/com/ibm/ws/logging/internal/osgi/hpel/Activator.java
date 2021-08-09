/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi.hpel;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.logging.hpel.config.HpelConfigurator;

/**
 * Activator for the HPEL bundle.
 */
public class Activator implements BundleActivator {
    private AbstractHPELConfigService[] hpelConfigService = null;

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext context) throws Exception {
        hpelConfigService =
                        new AbstractHPELConfigService[]
                        {
                         new AbstractHPELConfigService(context, "com.ibm.ws.logging.binaryLog") {
                             @Override
                             void forwardUpdated(Map<String, Object> map) {
                                 HpelConfigurator.updateLog(map);
                             }
                         },
                         new AbstractHPELConfigService(context, "com.ibm.ws.logging.binaryTrace") {
                             @Override
                             void forwardUpdated(Map<String, Object> map) {
                                 HpelConfigurator.updateTrace(map);
                             }
                         }
//                                           ,
//                                           new AbstractHPELConfigService(context, "com.ibm.ws.logging.textLog") {
//                                               @Override
//                                               void forwardUpdated(Map<String, Object> map) {
//                                                   HpelConfigurator.updateText(map);
//                                               }
//
//                                           }
                        };
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext context) throws Exception {
        if (hpelConfigService != null) {
            for (AbstractHPELConfigService service : hpelConfigService) {
                service.stop();
            }
            hpelConfigService = null;
        }
    }

}
