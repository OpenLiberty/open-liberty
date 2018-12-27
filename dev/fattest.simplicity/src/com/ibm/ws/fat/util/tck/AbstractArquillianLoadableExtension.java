 /*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * This class is a central repository of modifications we make at runtime to tck jars. 
 * To use this class extend this class in your tck project, registering the subclass in
 * META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension then impliment the
 * method getModifications to return a set of all desired modifications.
 *
 * The available modifications can be found in TCKArchiveModifications. 
 */
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
