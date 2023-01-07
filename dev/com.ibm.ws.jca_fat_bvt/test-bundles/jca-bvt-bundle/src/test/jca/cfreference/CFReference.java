/*******************************************************************************
 * Copyright (c) 2016,2022 IBM Corporation and others.
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
package test.jca.cfreference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.resource.ResourceFactory;

/**
 *
 */
@Component(name = "test.jca.cfreference",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true)
public class CFReference {

    @Reference
    protected ResourceFactory jcaConnectionFactory;

    protected void activate() {
        System.out.println("CFReference successfully bound resource factory");
    }

}
