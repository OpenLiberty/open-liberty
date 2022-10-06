/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.repository;

/**
 * Method signatures are copied from the jakarta.data.repository.Limit from the Jakarta Data repo.
 */
public class Limit {
    private final long max, start;

    private Limit(long startAt, long maxResults) {
        start = startAt;
        max = maxResults;
        if (start < 1 || max < 1)
            throw new IllegalArgumentException();
    }

    public long maxResults() {
        return max;
    }

    public static Limit of(long maxResults) {
        return new Limit(1L, maxResults);
    }

    public static Limit of(long maxResults, long startAt) {
        return new Limit(startAt, maxResults);
    }

    public long startAt() {
        return start;
    }
}