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
package jakarta.data;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Method signatures copied from the Jakarta Data git repo.
 */
@FunctionalInterface
public interface Streamable<T> extends Iterable<T> {
    public default Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(),
                                    false /* not parallel */);
    }
}
