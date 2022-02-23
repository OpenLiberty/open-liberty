/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.repo;

/**
 * Pointer to a feature repository.
 */
public class FeatureRepository extends Repository {
    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = FeatureRepository.class.getSimpleName();

    public void log(String message) {
        log(CLASS_NAME, message);
    }

    //

    public FeatureRepository(String imagePath, String path) throws Exception {
        super(imagePath, path);
    }
}
