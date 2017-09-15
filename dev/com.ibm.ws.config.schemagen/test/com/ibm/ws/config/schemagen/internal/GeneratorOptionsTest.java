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
package com.ibm.ws.config.schemagen.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GeneratorOptionsTest {

	@Test
	public void testLocalePortuguese() {
		runTest("pt", "BR");
	}

	@Test
	public void testLocaleChinese() {
		runTest("zh", null);
	}

	@Test
	public void testLocaleTraditionalChinese() {
		runTest("zh", "TW");
	}
	
	private void runTest(String lang, String country) {
		String locale = lang + '_' + country;
		
		if (country == null) locale = lang;
		
		GeneratorOptions options = new GeneratorOptions();
		options.processArgs(new String[] {"--locale=" + locale, "file.xsd"});
		
		assertEquals("The language should be pt", lang, options.getLocale().getLanguage());
		if (country != null) {
			assertEquals("The country should be BR", country, options.getLocale().getCountry());
		} else {
			assertEquals("The country should be blank", "", options.getLocale().getCountry());
		}
	}
}
