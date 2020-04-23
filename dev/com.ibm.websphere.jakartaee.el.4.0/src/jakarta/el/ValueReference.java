/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates and others.
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

import java.io.Serializable;

/**
 * This encapsulates a base model object and one of its properties.
 *
 * @since Jakarta Expression Language 2.2
 */
public class ValueReference implements Serializable {

    private static final long serialVersionUID = -4076659531951367109L;

    private Object base;
    private Object property;

    public ValueReference(Object base, Object property) {
        this.base = base;
        this.property = property;
    }

    public Object getBase() {
        return base;
    }

    public Object getProperty() {
        return property;
    }

}
