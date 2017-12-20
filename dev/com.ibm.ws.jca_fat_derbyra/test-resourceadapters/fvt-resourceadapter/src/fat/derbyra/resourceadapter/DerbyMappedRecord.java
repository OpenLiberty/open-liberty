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
package fat.derbyra.resourceadapter;

import java.util.TreeMap;

import javax.resource.cci.MappedRecord;

@SuppressWarnings("rawtypes")
public class DerbyMappedRecord extends TreeMap implements MappedRecord {
    private static final long serialVersionUID = 1L;

    private String desc;
    private String name;

    @Override
    public String getRecordName() {
        return name;
    }

    @Override
    public String getRecordShortDescription() {
        return desc;
    }

    @Override
    public void setRecordName(String name) {
        this.name = name;
    }

    @Override
    public void setRecordShortDescription(String desc) {
        this.desc = desc;
    }
}