/*******************************************************************************
* Copyright (c) 2017, 2023 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-2.0/
* 
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.util.Set;

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

    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        Set<TCKArchiveModifications> modifications = getModifications();
        for (TCKArchiveModifications modifier : modifications) {
            modifier.applyModification(extensionBuilder);
        }
    }

    public abstract Set<TCKArchiveModifications> getModifications();
}
