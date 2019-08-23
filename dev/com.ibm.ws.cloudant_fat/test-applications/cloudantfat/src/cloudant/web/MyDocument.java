/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cloudant.web;

/**
 * Cloudant document
 */
public class MyDocument {
    private String _id;

    private boolean bValue;

    private int iValue;

    private String sValue;

    public String getId() {
        return _id;
    }

    public boolean getBValue() {
        return bValue;
    }

    public int getIValue() {
        return iValue;
    }

    public String getSValue() {
        return sValue;
    }

    public void setId(String id) {
        _id = id;
    }

    public void setBValue(boolean b) {
        this.bValue = b;
    }

    public void setIValue(int i) {
        this.iValue = i;
    }

    public void setSValue(String s) {
        this.sValue = s;
    }
}
