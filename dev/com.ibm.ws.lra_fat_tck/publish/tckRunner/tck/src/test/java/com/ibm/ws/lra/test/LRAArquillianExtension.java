/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.lra.test;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.core.spi.LoadableExtension.ExtensionBuilder;

/**
 *
 */
public class LRAArquillianExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.service(ApplicationArchiveProcessor.class, LRATckArchiveProcessor.class);
    }
}


/*

package com.ibm.ws.fat.util.tck;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.core.spi.LoadableExtension;

public abstract class AbstractArquillianLoadableExtension implements LoadableExtension {

   private static final Logger LOG = Logger.getLogger(AbstractArquillianLoadableExtension.class.getName());

   @Override
   public void register(ExtensionBuilder extensionBuilder) {
       Set<TCKArchiveModifications> modifications = getModifications();
       for (TCKArchiveModifications modifier : modifications) {
           modifier.applyModification(extensionBuilder);
       }
   }

   public abstract Set<TCKArchiveModifications> getModifications();
}
*/
