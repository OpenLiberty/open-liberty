/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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

package com.ibm.ws.repository.transport.model;

import java.util.Calendar;

public class Reviewed extends AbstractJSON {

    private User by;
    private Calendar on;

    public Reviewed() {
    }

    /**
     * Copy constructor
     *
     * @param other the object to copy
     */
    public Reviewed(Reviewed other) {
        this.by = CopyUtils.copyObject(other.by, User::new);
        this.on = CopyUtils.copyCalendar(other.on);
    }

    public User getBy() {
        return by;
    }

    public void setBy(User by) {
        this.by = by;
    }

    public Calendar getOn() {
        return on;
    }

    public void setOn(Calendar on) {
        this.on = on;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((by == null) ? 0 : by.hashCode());
        result = prime * result + ((on == null) ? 0 : on.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Reviewed other = (Reviewed) obj;
        if (by == null) {
            if (other.by != null)
                return false;
        } else if (!by.equals(other.by))
            return false;
        if (on == null) {
            if (other.on != null)
                return false;
        } else if (!on.equals(other.on))
            return false;
        return true;
    }

}
