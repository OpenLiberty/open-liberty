/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.migrator;

import java.util.Set;

import com.ibm.ws.featureverifier.migrator.IgnoreMigrator.Section;

abstract class Ignore {
    protected final String feature;
    private final Section section;

    public Ignore(Section s, String feature) {
        this.section = s;
        this.feature = feature;
    }

    /**
     * @return
     */
    public Section getSection() {
        return this.section;
    }

    /**
     * @param aggregate
     * @return
     */
    public boolean appliesTo(Set<String> aggregate) {
        // If the feature name is null, include it in every config
        if (feature == null)
            return true;

        return aggregate.contains(feature);
    }
}