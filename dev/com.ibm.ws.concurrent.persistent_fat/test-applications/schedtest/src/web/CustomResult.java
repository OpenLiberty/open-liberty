/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.Serializable;

public class CustomResult implements Serializable {
    private static final long serialVersionUID = 3260881763627493460L;

    int part1;
    int part2;

    CustomResult(int part1, int part2) {
        this.part1 = part1;
        this.part2 = part2;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CustomResult && part1 == ((CustomResult) obj).part1 && part2 == ((CustomResult) obj).part2;
    }

    @Override
    public int hashCode() {
        return part1 + part2;
    }

    @Override
    public String toString() {
        return super.toString() + '(' + part1 + ',' + part2 + ')';
    }
}
