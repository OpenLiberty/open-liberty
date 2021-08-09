/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.runner.RunWith;

import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;

/**
 * Same as {@link SimpleTest}, but uses the acme://* URI instead of an HTTPS
 * URI.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // No value added
public class AcmeURISimpleTest extends AcmeSimpleTest {

	@Override
	protected boolean useAcmeURIs() {
		return true;
	}

	@Override
	protected void stopServer(String... msgs) throws Exception {
		String os = System.getProperty("os.name").toLowerCase();
		if (JavaInfo.JAVA_VERSION > 8 && !os.startsWith("z/os")) {
			AcmeFatUtils.stopServer(server, msgs);
		} else {
			/*
			 * HttpConnector.config runs oddly slow on Java 8 and z/OS and can trigger the update
			 * timeout warning
			 */
			List<String> tempList = new ArrayList<String>(Arrays.asList(msgs));
			tempList.add("CWWKG0027W");
			AcmeFatUtils.stopServer(server, tempList.toArray(new String[tempList.size()]));
		}
	}
}
