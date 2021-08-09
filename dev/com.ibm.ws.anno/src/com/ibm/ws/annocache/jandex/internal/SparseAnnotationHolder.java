/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.jandex.internal;

/**
 * Data recording an annotation target (class, field, or method)
 * along with the annotations of that target.
 */
public class SparseAnnotationHolder{

    public SparseAnnotationHolder(SparseDotName name, SparseDotName[] annotations) {
        this.name = name;
        this.annotations = annotations;
    }

    //

    private final SparseDotName name;

    public SparseDotName getName() {
        return name;
    }

    //

    private final SparseDotName[] annotations;

    public SparseDotName[] getAnnotations() {
        return annotations;
    }
}