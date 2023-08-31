/*******************************************************************************
 * Copyright (c) 2013, 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.messaging.JMS20.fat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.topology.impl.LibertyServer;


@SkipIfCheckpointNotSupported
@RunWith(Suite.class)
@SuiteClasses({
                  DummyTest.class,
                  BucketSet1CpClientTest.class,
                  BucketSet2CpClientTest.class,
                  BucketSet1CpEngineTest.class,
                  BucketSet2CpEngineTest.class,

})

public class FATSuite {

	public static void addServerEnvPorts (LibertyServer server, ArrayList<PortSetting> portSettings ) {		
		
		File serverEnvFile = new File(server.getServerRoot() + "/server.env");
		try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile, true))) {
			portSettings.forEach((setting) -> {
               Integer port = Integer.getInteger(setting.lookupName, setting.defaultValue); 
               serverEnvWriter.println(setting.newEnvName+"="+port);
	        });
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public static class PortSetting {
		
		PortSetting(String name, int def, String newName) {
			lookupName=name;
			defaultValue=def;
			newEnvName=newName;
		}
		
		public String lookupName;
		public int defaultValue;
		public String newEnvName;
	}

}


