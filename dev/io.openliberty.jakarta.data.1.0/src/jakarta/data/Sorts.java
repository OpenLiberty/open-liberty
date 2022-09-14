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
package jakarta.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Copied from jakarta.nosql.mapping.Sorts to investigate how well the
 * JNoSQL repository-related classes work for relational database access.
 * Unable to copy the of method because it depends on a JNoSQL class, ServiceLoaderProvider.
 */
public interface Sorts {
    Sorts asc(String name);

    Sorts desc(String name);

    Sorts add(Sort sort);

    Sorts remove(Sort sort);

    List<Sort> getSorts();

    static Sorts sorts() {
        // We don't have the JNoSQL ServiceLoaderProvider here.
        return new SortsImpl.Provider().get();
    }

    interface SortsProvider extends Supplier<Sorts> {
    }
}

/**
 * Fake implementation:
 */
class SortsImpl implements Sorts {
    private final List<Sort> sorts = new ArrayList<>();

    private SortsImpl() {
    }

    @Override
    public Sorts asc(String name) {
        sorts.add(Sort.asc(name));
        return this;
    }

    @Override
    public Sorts desc(String name) {
        sorts.add(Sort.desc(name));
        return this;
    }

    @Override
    public Sorts add(Sort sort) {
        sorts.add(sort);
        return this;
    }

    @Override
    public Sorts remove(Sort sort) {
        sorts.remove(sort);
        return this;
    }

    @Override
    public List<Sort> getSorts() {
        return new ArrayList<>(sorts);
    }

    public static class Provider implements Sorts.SortsProvider {
        @Override
        public Sorts get() {
            return new SortsImpl();
        }
    }
}
