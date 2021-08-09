/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.common.enums;

import java.util.Collection;
import java.util.Collections;

/**
 * The attributes that you can filter a resource by.
 * NB Not all resources have all attributes.
 */
public enum FilterableAttribute {

    TYPE("type", ResourceType.class),
    PRODUCT_ID("wlpInformation.appliesToFilterInfo.productId", String.class),
    /** When filtering by VISIBILITY only one value should be provided. */
    VISIBILITY("wlpInformation.visibility", "wlpInformation2.visibility", Collections.singleton(Visibility.INSTALL.toString()), Visibility.class),
    PRODUCT_MIN_VERSION("wlpInformation.appliesToFilterInfo.minVersion.value", String.class),
    PRODUCT_HAS_MAX_VERSION("wlpInformation.appliesToFilterInfo.hasMaxVersion", Boolean.class),
    SYMBOLIC_NAME("wlpInformation.provideFeature", String.class),
    SHORT_NAME("wlpInformation.shortName", String.class),
    LOWER_CASE_SHORT_NAME("wlpInformation.lowerCaseShortName", String.class),
    VANITY_URL("wlpInformation.vanityRelativeURL", String.class);
    private final String attributeName;
    private final String secondaryAttributeName;
    private final Collection<String> valuesInSecondaryAttributeName;
    private final Class<?> type;

    private FilterableAttribute(final String attributeName, Class<?> type) {
        this.attributeName = attributeName;
        this.secondaryAttributeName = null;
        this.valuesInSecondaryAttributeName = null;
        this.type = type;
    }

    private FilterableAttribute(final String attributeName, final String secondaryAttributeName,
                                final Collection<String> valuesInSecondaryAttributeName, Class<?> type) {
        this.attributeName = attributeName;
        this.secondaryAttributeName = secondaryAttributeName;
        this.valuesInSecondaryAttributeName = valuesInSecondaryAttributeName;
        this.type = type;
    }

    /**
     * @return the attributeName
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * @return the secondaryAttributeName
     */
    public String getSecondaryAttributeName() {
        return secondaryAttributeName;
    }

    /**
     * @return the valuesInSecondaryAttributeName
     */
    public Collection<String> getValuesInSecondaryAttributeName() {
        return valuesInSecondaryAttributeName;
    }

    public Class<?> getType() {
        return type;
    }
}