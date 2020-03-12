/*
 * Copyright (c) 2013, 2019 Oracle and/or its affiliates and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.el;

/**
 * A runtime representation of a Class in the Jakarta Expression Language expressions. It encapsulates the
 * java.lang.Class instance.
 *
 * <p>
 * This class is used only in {@link StaticFieldELResolver} and will probably only be of interest to Jakarta Expression
 * Language implementors, and not Jakarta Expression Language users.
 *
 * @since Jakarta Expression Language 3.0
 */
public class ELClass {

    private Class<?> klass;

    /**
     * Constructor
     *
     * @param klass The Class instance
     */
    public ELClass(Class<?> klass) {
        this.klass = klass;
    }

    /**
     * Returns the Class instance
     *
     * @return The Class instance
     */
    public Class<?> getKlass() {
        return this.klass;
    }
}
