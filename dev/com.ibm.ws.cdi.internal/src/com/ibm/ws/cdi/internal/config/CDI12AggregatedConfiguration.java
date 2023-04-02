/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.internal.config;

/**
 * An abstract class that overrides some methods related to the cdiemptyBeansXmlCDI3Compatibility property.
 */
public abstract class CDI12AggregatedConfiguration extends AggregatedConfiguration {
    @Override
    /**
     * emptyBeansXmlCDI3Compatibility is only supported on CDI 4.0 and above so this implementation ignores it if
     * if it is set. This implementation of the emptyBeansXmlCDI3Compatibility() method should never be called.
     */
    public void setCdiConfig(Boolean enableImplicitBeanArchives, Boolean emptyBeansXmlCDI3Compatibility) {
        //The emptyBeansXmlCDI3Compatibility attribute of the cdi configuration element is supported only on CDI 4.0 or newer. This attribute is ignored.
        super.setCdiConfig(enableImplicitBeanArchives, null);
    }

    @Override
    public boolean emptyBeansXmlCDI3Compatibility() {
        throw new UnsupportedOperationException();
    }
}
