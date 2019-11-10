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

class Comment extends Ignore {

    private final String comment;
    private final MissingOptions missingOptions;

    /**
     * @param value
     * @param feature
     * @param ifMissing
     */
    public Comment(Section s, String feature, String value, MissingOptions missingOptions) {
        super(s, feature);
        this.comment = value;
        this.missingOptions = missingOptions;
    }

    @Override
    public String toString() {
        return "  <!-- " + comment + " -->";
    }

    @Override
    public boolean appliesTo(Set<String> aggregate) {
        if (missingOptions != null && !missingOptions.appliesTo(aggregate))
            return false;

        return super.appliesTo(aggregate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null)
            return false;

        if (!(o instanceof Comment))
            return false;

        Comment c = (Comment) o;

        return c.toString().equals(toString());

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((comment == null) ? 0 : comment.hashCode());
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());

        return result;
    }

}