/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.classsource.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;

/**
 * Standard implementation of class source options.
 */
public class ClassSourceImpl_Options implements ClassSource_Options {
    private static final String JANDEX_OVERRIDE_PROPERTY_NAME = "com.ibm.ws.jandex.enable";
    private static final Boolean JANDEX_OVERRIDE;

    static {
        String jandexOverrideText = JandexLogger.getProperty(JANDEX_OVERRIDE_PROPERTY_NAME);
        JANDEX_OVERRIDE = (jandexOverrideText == null) ? null : Boolean.valueOf(jandexOverrideText);
    }

    //

    public static final boolean USE_JANDEX_DEFAULT = false;

    //

    @Trivial
    public ClassSourceImpl_Options() {
        this.useJandex = null;
    }

    public ClassSourceImpl_Options(boolean useJandex) {
        this.useJandex = Boolean.valueOf(useJandex);
    }

    @Override
    @Trivial
    public String toString() {
        return super.toString() + "(Base " + useJandex + ", Override " + JANDEX_OVERRIDE + ")";
    }

    @Override
    @Trivial
    public boolean getUseJandexDefault() {
        return USE_JANDEX_DEFAULT;
    }

    //

    private Boolean useJandex;

    @Override
    public void setUseJandex(boolean useJandex) {
        this.useJandex = Boolean.valueOf(useJandex);
    }

    @Override
    public void unsetUseJandex() {
        this.useJandex = null;
    }

    @Override
    @Trivial
    public boolean getIsSetUseJandex() {
        return ( useJandex != null );
    }

    @Override
    @Trivial
    public boolean getUseJandex() {
    	if ( JANDEX_OVERRIDE != null ) {
    		return JANDEX_OVERRIDE.booleanValue();
    	} else if ( useJandex != null ) {
    		return useJandex.booleanValue();
    	} else {
    		return USE_JANDEX_DEFAULT;
    	}
    }
}
