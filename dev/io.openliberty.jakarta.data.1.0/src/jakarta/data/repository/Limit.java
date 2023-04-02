/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.repository;

/**
 * Method signatures are copied from the jakarta.data.repository.Limit from the Jakarta Data repo.
 */
public class Limit {
    private final int max;
    private final long start;

    private Limit(long startAt, int maxResults) {
        start = startAt;
        max = maxResults;
        if (start < 1)
            throw new IllegalArgumentException("startAt: " + start);
        if (max < 1)
            throw new IllegalArgumentException("maxResults: " + max);
    }

    public int maxResults() {
        return max;
    }

    public static Limit of(int maxResults) {
        return new Limit(1L, maxResults);
    }

    public static Limit range(long startAt, long endAt) {
        if (startAt >= endAt)
            throw new IllegalArgumentException("startAt: " + startAt + ", endAt: " + endAt);

        if (Integer.MAX_VALUE <= endAt - startAt)
            throw new IllegalArgumentException("startAt: " + startAt + ", endAt: " + endAt + ", maxResults > " + Integer.MAX_VALUE);

        return new Limit(startAt, 1 + (int) (endAt - startAt));
    }

    public long startAt() {
        return start;
    }
}