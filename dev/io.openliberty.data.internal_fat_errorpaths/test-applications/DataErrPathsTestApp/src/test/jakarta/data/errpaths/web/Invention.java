/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.errpaths.web;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * An invalid entity - because it lacks an Id attribute.
 */
@Entity
public class Invention {

    @Column(nullable = false)
    public int disclosureNum;

    public int patentNum;

    @Column(nullable = false)
    public String title;

    public Invention() {
    }

    public Invention(int disclosureNum, int patentNum, String title) {
        this.disclosureNum = disclosureNum;
        this.patentNum = patentNum;
        this.title = title;
    }

    @Override
    public String toString() {
        return "Invention#" + disclosureNum + "(" + patentNum + "): " + title;
    }
}
