/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.generator.ejbext;

import java.io.File;

import com.ibm.ws.javaee.ddmetadata.generator.util.AbstractEJBJarBndExtModelAdapterClassGenerator;
import com.ibm.ws.javaee.ddmetadata.model.Model;

public class EJBJarExtModelAdapterClassGenerator extends AbstractEJBJarBndExtModelAdapterClassGenerator {
    public EJBJarExtModelAdapterClassGenerator(File destdir, Model model) {
        super(destdir, model);
    }

    @Override
    protected String getType() {
        return "ext";
    }
}
