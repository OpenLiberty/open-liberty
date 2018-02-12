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
	private static final String CLASS_NAME = ClassSourceImpl_Options.class.getSimpleName();
    
    private static final String JANDEX_OVERRIDE_PROPERTY_NAME = "com.ibm.ws.jandex.enable";
    private static final boolean JANDEX_OVERRIDE_DEFAULT_VALUE = false;
    
    private static final boolean JANDEX_OVERRIDE =
    	JandexLogger.getProperty(
    		CLASS_NAME, "<static init>",
    		JANDEX_OVERRIDE_PROPERTY_NAME, JANDEX_OVERRIDE_DEFAULT_VALUE);
    
    public boolean getJandexOverride() {
    	return JANDEX_OVERRIDE;
    }

    //

	@Trivial
	public ClassSourceImpl_Options() {
		this.isSetUseJandex = false;
		this.useJandex = ( getJandexOverride() ? true : USE_JANDEX_DEFAULT );
	}

	@Trivial
	public String toString() {
		return super.toString() +
				"(" +
				Boolean.toString(useJandex) + 
				(isSetUseJandex ? " [Set]" : " [Unset]") +
				")";
	}

	public static final boolean USE_JANDEX_DEFAULT = false;

	/**
	 * Answer the default 'use jandex' value.  This implementation
	 * always answers false.
	 * 
	 * @return The default 'use jandex value'.
	 */
	@Override
	@Trivial
	public boolean getUseJandexDefault() {
		return USE_JANDEX_DEFAULT;
	}

	//

	private boolean isSetUseJandex;
	private boolean useJandex;

	@Override
	@Trivial
	public boolean getIsSetUseJandex() {
		return isSetUseJandex;
	}

	@Override
	@Trivial
	public boolean getUseJandex() {
		return useJandex;
	}

	@Override
	public void setUseJandex(boolean useJandex) {
		this.isSetUseJandex = true;
		this.useJandex = useJandex;
	}

	@Override
	public void unsetUseJandex() {
		this.useJandex = USE_JANDEX_DEFAULT;
		this.isSetUseJandex = false;
	}
}
