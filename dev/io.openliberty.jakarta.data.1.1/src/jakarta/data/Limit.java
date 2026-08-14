/*******************************************************************************
 * Copyright (c) 2022,2026 IBM Corporation and others.
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
package jakarta.data;

import jakarta.data.messages.Messages;
import jakarta.annotation.Nonnull;

/**
 * Method signatures are copied from the jakarta.data.repository.Limit from the Jakarta Data repo.
 */
public record Limit(int maxResults,
                long startAt) {

    public Limit {
        if (startAt < 1)
            throw new IllegalArgumentException("startAt: " + startAt);
        if (maxResults < 1)
            throw new IllegalArgumentException("maxResults: " + maxResults);
    }

    @Nonnull
    public static Limit of(int maxResults) {
        return new Limit(maxResults, 1L);
    }

    @Nonnull
    public static Limit of(int maxResults, long offset) {
        if (offset < 0)
            throw new IllegalArgumentException(Messages.get("004.arg.negative",
                                                            "offset"));

        if (offset == Long.MAX_VALUE)
            throw new IllegalArgumentException(Messages.get("013.arg.invalid",
                                                            "offset",
                                                            offset));

        return new Limit(maxResults, offset + 1);
    }

    @Nonnull
    public static Limit range(long startAt, long endAt) {
        if (startAt > endAt)
            throw new IllegalArgumentException("startAt: " + startAt +
                                               ", endAt: " + endAt);

        if (Integer.MAX_VALUE <= endAt - startAt)
            throw new IllegalArgumentException("startAt: " + startAt +
                                               ", endAt: " + endAt +
                                               ", maxResults > " + Integer.MAX_VALUE);

        return new Limit(1 + (int) (endAt - startAt), startAt);
    }
}