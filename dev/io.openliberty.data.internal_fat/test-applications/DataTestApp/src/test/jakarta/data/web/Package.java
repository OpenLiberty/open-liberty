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
package test.jakarta.data.web;

/**
 *
 */
public class Package {

    public String description;

    public int id;

    public float length, width, height;

    @Override
    public String toString() {
        return "Package type=" + id + "; L=" + length + "; W=" + width + " H=" + height + " " + description;
    }
}
