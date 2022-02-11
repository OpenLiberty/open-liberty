/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.internal.config;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * An abstract class that overrides some methods related to the cdiEmptyBeansXMLExplicitArchive property.
 * emptyBeansXMLExplicitArchive is only supported on CDI 4.0 and above so this implementation outputs a warning
 * if it is set. This implementation of the emptyBeansXMLExplicitArchive() method should never be called.
 */
public abstract class CDI12AggregatedConfiguration extends AggregatedConfiguration {
    private static final TraceComponent tc = Tr.register(CDI12AggregatedConfiguration.class);

    @Override
    public void setCdiConfig(Boolean enableImplicitBeanArchives, Boolean emptyBeansXMLExplicitArchive) {
        if (tc.isWarningEnabled() && emptyBeansXMLExplicitArchive != null) {
            //CWOWB1013W: The attribute cdiEmptyBeansXMLExplicitArchive of element type cdi is only supported on CDI 4.0 and newer. The attribute will be ignored.
            Tr.warning(tc, "cdiEmptyBeansXMLExplicitArchive.ignored.CWOWB1016W");
        }
        super.setCdiConfig(enableImplicitBeanArchives, null);
    }

    @Override
    public boolean emptyBeansXMLExplicitArchive() {
        throw new UnsupportedOperationException();
    }
}
