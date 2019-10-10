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
package com.ibm.wsspi.anno.classsource;

/**
 * Options for class sources.
 * 
 * See {@link ClassSource_Factory#createAggregateClassSource(String, ClassSource_Options)}.
 * 
 * Only one option is supported: Whether Jandex indexes are to be used when populating
 * annotations targets tables.
 * 
 * That is, when processing a non-aggregate class source, when Jandex use is enabled,
 * steps to populate annotation targets first check for a Jandex index in a standard
 * location.  That is 'META-INF/jandex.idx'.
 * 
 * An adjustment is made when scanning a container mapped class source: If the container
 * is not a root container and has the relative path 'WEB-INF/classes', the location
 * of the Jandex index is adjusted to "../../META-INF/jandex.idx', which shifts the
 * location outside of the immediate container and back to being relative to the
 * container root.
 */
public interface ClassSource_Options {
	// JANDEX usags:

	/**
	 * Answer the default 'use jandex' setting.
	 * 
	 * @return The default 'use jandex' setting.
	 */
	boolean getUseJandexDefault();

	/**
	 * Tell if 'use jandex' is set.  If unset, the
	 * default value is returned from {@#getUseJandex()}.
	 * 
	 * @return Whether 'use jandex' is set.
	 */
	boolean getIsSetUseJandex();

	/**
	 * Set the 'use jandex' value.
	 * 
	 * @param useJandex The value to set to 'use jandex'.
	 */
	void setUseJandex(boolean useJandex);
	
	/**
	 * Unset the 'use jandex' value.
	 */
	void unsetUseJandex();

	/**
	 * Answer the 'use jandex' value.  If unset, the
	 * default value will be returned.
	 * 
	 * @return The 'use jandex' value.
	 */
	boolean getUseJandex();
}
