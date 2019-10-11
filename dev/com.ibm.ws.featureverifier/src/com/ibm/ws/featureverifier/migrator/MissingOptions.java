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

/**
 *
 */
public class MissingOptions {

    private final String[] onlyIfMissing;
    private final String[] onlyWhenIncluded;

    MissingOptions(String missing, String included) {
        if (missing == null) {
            onlyIfMissing = new String[0];
        } else {
            this.onlyIfMissing = missing.split(",");
        }

        if (included == null) {
            this.onlyWhenIncluded = new String[0];
        } else {
            this.onlyWhenIncluded = included.split(",");
        }
    }

    /**
     * @param aggregate
     * @return
     */
    boolean appliesTo(Set<String> aggregate) {
        for (String ifMissing : onlyIfMissing) {
            if (aggregate.contains(ifMissing))
                return false;
        }

        for (String included : onlyWhenIncluded) {
            if (aggregate.contains(included))
                return true;
        }

        // If length > 0, we failed to find a whenIncluded above.
        // If length == 0, no whenIncluded is specified, so we're good. 
        return (onlyWhenIncluded.length == 0);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (onlyIfMissing.length > 0) {
            builder.append(" ifMissing=");
            builder.append(IgnoreConstants.QUOTE);
            for (String missing : onlyIfMissing) {
                builder.append(missing);
                builder.append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(IgnoreConstants.QUOTE);
        }

        if (onlyWhenIncluded.length > 0) {
            builder.append(" whenIncluded=");
            builder.append(IgnoreConstants.QUOTE);
            for (String included : onlyWhenIncluded) {
                builder.append(included);
                builder.append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append(IgnoreConstants.QUOTE);
        }

        return builder.toString();
    }
}
