/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.util;

import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.security.wim.model.Entity;
import com.ibm.wsspi.security.wim.model.SortControl;
import com.ibm.wsspi.security.wim.model.SortKeyType;

/**
 *
 * Class to sort entities
 *
 */
public class SortHandler {

    private SortControl sortControl = null;

    /**
     * Constructor for SortHandler.
     */
    @Trivial
    public SortHandler() {
        super();
    }

    /**
     * Constructor for SortHandlerData.
     *
     * @param desiredSortControl the sort control object
     */
    @Trivial
    public SortHandler(SortControl sortControl) {
        super();
        this.sortControl = sortControl;
    }

    /**
     * Compares the two entity data objects.
     *
     * @param member1 the first member object to be compared
     * @param member2 the second member object to be compared
     * @return a negative integer, zero, or a positive integer as the first member object is less than,
     *         equal to, or greater than the second.
     */
    public int compareEntitysWithRespectToProperties(Entity entity1, Entity entity2) {
        List<SortKeyType> sortKeys = sortControl.getSortKeys();

        int temp = 0;
        for (int i = 0; i < sortKeys.size() && temp == 0; i++) {
            SortKeyType sortKey = (SortKeyType) sortKeys.get(i);
            String propName = sortKey.getPropertyName();
            boolean ascendingSorting = sortKey.isAscendingOrder();
            Object propValue1 = getPropertyValue(entity1, propName, ascendingSorting);
            Object propValue2 = getPropertyValue(entity2, propName, ascendingSorting);
            temp = compareProperties(propValue1, propValue2);
            if (!ascendingSorting) {
                temp = 0 - temp;
            }
        }
        return temp;
    }

    /**
     * Sets the SortControl object to the class
     *
     * @param sortControl the SortControl object which includes the sort keys,and other sorting related information
     */
    @Trivial
    public void setSortControl(SortControl sortControl) {
        this.sortControl = sortControl;
    }

    /**
     * Gets the SortControl object
     *
     * @return the SortControl object which is used for sorting
     */
    @Trivial
    public SortControl getSortControl() {
        return sortControl;
    }

    /**
     * Sorts the set of Member Objects
     *
     * @param members a set of Member objects to be sorted
     * @return a list of sorted Member objects
     */
    public List<Entity> sortEntities(List<Entity> entities) {
        if (entities != null && entities.size() > 0) {
            Entity[] ents = (Entity[]) entities.toArray(new Entity[entities.size()]);
            WIMSortCompare<Entity> wimSortComparator = new WIMSortCompare<Entity>(sortControl);
            Arrays.sort(ents, wimSortComparator);
            entities.clear();
            for (int i = 0; i < ents.length; i++) {
                entities.add(ents[i]);
            }
        }
        return entities;
    }

    private Object getPropertyValue(Entity entity, String propName, boolean ascending) {
        Object value = null;
        Object propValue = null;
        try {
            propValue = entity.get(propName);
        } catch (IllegalArgumentException e) {
            value = null;
        }

        if (propValue != null && !(propValue instanceof List)) {
            value = propValue;
        } else if (propValue != null) {
            List<?> props = (List<?>) propValue;
            if (props.size() > 0) {
                value = props.get(0);
                for (int i = 1; i < props.size(); i++) {
                    int temp = compareProperties(value, props.get(i));
                    if (temp > 0 && ascending) {
                        value = props.get(i);
                    }
                }
            }
        }
        return value;
    }

    private int compareProperties(Object prop1, Object prop2) {
        Collator localeCollator = null;
        int returnCode = 0;
        if (sortControl != null) {
            String localeStr = sortControl.getLocale();
            Locale locale = Locale.getDefault();
            if (localeStr != null) {
                int index = localeStr.indexOf("-");
                if (index != -1) {
                    String lang = localeStr.substring(0, index);
                    String country = localeStr.substring(index + 1);
                    locale = new Locale(lang, country);
                } else {
                    locale = new Locale(localeStr);
                }
            }
            if (locale != null) {
                localeCollator = Collator.getInstance(locale);
                localeCollator.setStrength(Collator.IDENTICAL);
            }

            if (prop1 == null && prop2 == null) {
                returnCode = 0;
            } else if (prop1 == null) {
                returnCode = -1;
            } else if (prop2 == null) {
                returnCode = 1;
            } else {
                if (prop1 instanceof String) {
                    returnCode = localeCollator.compare(prop1, prop2);
                } else if (prop1 instanceof Integer) {
                    returnCode = ((Integer) prop1).compareTo((Integer) prop2);
                } else if (prop1 instanceof Long) {
                    returnCode = ((Long) prop1).compareTo((Long) prop2);
                } else if (prop1 instanceof Double) {
                    returnCode = ((Double) prop1).compareTo((Double) prop2);
                } else {
                    returnCode = localeCollator.compare(prop1.toString(), prop2.toString());
                }
            }
        }

        return returnCode;
    }
}
