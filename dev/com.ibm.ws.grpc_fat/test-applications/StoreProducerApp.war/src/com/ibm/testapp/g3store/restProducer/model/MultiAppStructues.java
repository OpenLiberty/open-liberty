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
package com.ibm.testapp.g3store.restProducer.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * @author anupag
 *
 */
public class MultiAppStructues {

    @Schema(required = false, type = SchemaType.ARRAY, implementation = AppStructure.class, description = "App structures")
    private List<AppStructure> structureList;

    public List<AppStructure> getStructureList() {
        return structureList;
    }

    public void setStructureList(List<AppStructure> structureList) {
        this.structureList = structureList;
    }

}
