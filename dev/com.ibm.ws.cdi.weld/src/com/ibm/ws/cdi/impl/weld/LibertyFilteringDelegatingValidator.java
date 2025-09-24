/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.cdi.impl.weld;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.bootstrap.BeanDeployment;
import org.jboss.weld.bootstrap.Validator;
import org.jboss.weld.manager.BeanManagerImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.impl.weld.validation.LibertyDelegatingValidator;
import com.ibm.ws.cdi.internal.archive.liberty.ExtensionArchiveImpl;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

//This class contains the common code that filters methods and passes them to the delegate.
//However because weld's Validator is a concrete class with no interface, this deligator
//needs to call its constructor.
//
//The method signature for the constructor changed across versions of weld, so we extend
//LibertyDelegatingValidatorConstructor which is in the versioned bundles. That class
//has a no-args constructor which calls Validator's constructor with the correct method
//signature.
public class LibertyFilteringDelegatingValidator extends LibertyDelegatingValidator {

    private static final TraceComponent tc = Tr.register(LibertyFilteringDelegatingValidator.class);

    private final Set<BeanManager> filteredBeanManagers = new HashSet<BeanManager>();

    public LibertyFilteringDelegatingValidator(Validator delegate, WebSphereCDIDeployment webSphereCDIDeployment) {
        super(delegate);
        WebSphereCDIDeploymentImpl deployment = (WebSphereCDIDeploymentImpl) webSphereCDIDeployment;
        for (WebSphereBeanDeploymentArchive bda : deployment.getRuntimeExtensionBDAs()) {

            CDIArchive archive = bda.getArchive();
            //The check for applicationBDAsVisible is necessary to avoid inadvertently filtering out LiteExtensions
            //as they are registered through a runtime extension containing the LiteExtensionTranslator.
            if (archive instanceof ExtensionArchiveImpl &&
                ((ExtensionArchiveImpl) archive).applicationBDAsVisible()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "<init>", "BeanManager {0} will not be validated", bda.getBeanManager());
                }
                filteredBeanManagers.add(bda.getBeanManager());
            }
        }
    }

    @Override
    public void validateDeployment(BeanManagerImpl manager, BeanDeployment deployment) {
        if (!filteredBeanManagers.contains(manager)) {
            super.validateDeployment(manager, deployment);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "validateDeployment", "Skipping BeanManager {0}", manager);
        }
    }
}
