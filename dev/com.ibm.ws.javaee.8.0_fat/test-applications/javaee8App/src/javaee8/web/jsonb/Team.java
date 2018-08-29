/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javaee8.web.jsonb;

public class Team {
    public String name;
    public int size;
    public float winLossRatio;

    @Override
    public String toString() {
        return "name=" + name + "  size=" + size + "  winLossRatio=" + winLossRatio;
    }
}