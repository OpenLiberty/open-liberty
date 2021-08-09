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
package com.ibm.ws.javaee.ddmodel.bval;

import com.ibm.ws.javaee.dd.bval.ValidationConfig;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.EntryAdapter;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

public class ValidationConfigEntryAdapter implements EntryAdapter<ValidationConfig> {

    @Override
    public ValidationConfig adapt(Container root,
                                  OverlayContainer rootOverlay,
                                  ArtifactEntry artifactEntry,
                                  Entry entryToAdapt) throws UnableToAdaptException {

        String path = artifactEntry.getPath();
        ValidationConfig validationConfig = (ValidationConfig) rootOverlay.getFromNonPersistentCache(path, ValidationConfig.class);
        if (validationConfig == null) {
            try {
                ValidationConfigDDParser ddParser = new ValidationConfigDDParser(root, entryToAdapt);
                validationConfig = ddParser.parse();
            } catch (ParseException e) {
                throw new UnableToAdaptException(e);
            }

            rootOverlay.addToNonPersistentCache(path, ValidationConfig.class, validationConfig);
        }

        return validationConfig;
    }

}
