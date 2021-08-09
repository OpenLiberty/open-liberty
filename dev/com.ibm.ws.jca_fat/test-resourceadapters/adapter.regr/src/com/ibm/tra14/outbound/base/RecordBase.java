/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra14.outbound.base;

//@SuppressWarnings("serial")
public class RecordBase implements javax.resource.cci.Record {

    private String personName = null;
    private int personNumber = 0;

    private String recordName = null;
    private String shortDescription = null;

    public RecordBase() {
        personName = "default";
        personNumber = 31;
        recordName = "default";
        shortDescription = "default description";
    }

    @Override
    public Object clone() {
        RecordBase newClone = new RecordBase();
        newClone.setPersonName(personName);
        newClone.setPersonNumber(personNumber);
        newClone.setRecordName(recordName);
        newClone.setRecordShortDescription(shortDescription);

        return newClone;
    }

    @Override
    public boolean equals(Object other) {
        RecordBase castedOther = null;
        try {
            castedOther = (RecordBase) other;
        } catch (ClassCastException cce) {
            return false;
        }
        if (personName.equals(castedOther.getPersonName()) &&
            (personNumber == getPersonNumber()) &&
            recordName.equals(castedOther.getRecordName()) &&
            shortDescription.equals(castedOther.getRecordShortDescription())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getRecordName() {
        return recordName;
    }

    @Override
    public String getRecordShortDescription() {
        return shortDescription;
    }

    public String getPersonName() {
        return personName;
    }

    public int getPersonNumber() {
        return personNumber;
    }

    @Override
    public int hashCode() {
        //Yeah, I know it's cheesy, but I don't actually plan on using this..
        return personNumber;
    }

    @Override
    public void setRecordName(String name) {
        recordName = name;
    }

    @Override
    public void setRecordShortDescription(String description) {
        shortDescription = description;
    }

    public void setPersonName(String perName) {
        personName = perName;
    }

    public void setPersonNumber(int perNumber) {
        personNumber = perNumber;
    }

}
