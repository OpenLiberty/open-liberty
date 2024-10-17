/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils.tck;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.utils.tck.TCKResultsInfo.TCKJarInfo;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;

public class TCKResultInfoTest {

    private static final TCKJarInfo VERSION_1_0_0 = new TCKJarInfo();

    @BeforeClass
    public static void setup() {
        VERSION_1_0_0.version = "1.0.0";
    }

    @Test
    public void getFullSpecNameTest() {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Config", null, VERSION_1_0_0);
        TCKResultsInfo jakarta = new TCKResultsInfo(Type.JAKARTA, "Data", null, VERSION_1_0_0);

        assertEquals("MicroProfile Config 1.0.0", microprofile.getFullSpecName());
        assertEquals("Jakarta Data 1.0.0", jakarta.getFullSpecName());
    }

    @Test
    public void getSpecNameForURLTest() throws Exception {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        TCKResultsInfo jakarta = new TCKResultsInfo(Type.JAKARTA, "Dependency Injection", null, VERSION_1_0_0);

        assertEquals("fault-tolerance", getSpecNameForURL().invoke(microprofile));
        assertEquals("dependency-injection", getSpecNameForURL().invoke(jakarta));
    }

    @Test
    public void getSpecURLTest() {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        TCKResultsInfo jakarta = new TCKResultsInfo(Type.JAKARTA, "Data", null, VERSION_1_0_0);

        assertEquals("https://github.com/eclipse/microprofile-fault-tolerance/tree/1.0.0", microprofile.getSpecURL());
        assertEquals("https://jakarta.ee/specifications/data/1.0.0", jakarta.getSpecURL());
    }

    @Test
    public void getTCKURLTest() {
        TCKResultsInfo microprofile = new TCKResultsInfo(Type.MICROPROFILE, "Fault Tolerance", null, VERSION_1_0_0);
        TCKResultsInfo jakartaWithOutVersion = new TCKResultsInfo(Type.JAKARTA, "Data", null, VERSION_1_0_0);
        TCKResultsInfo jakartaWithVersion = new TCKResultsInfo(Type.JAKARTA, "Dependency Injection", null, VERSION_1_0_0);
        jakartaWithVersion.withPlatformVersion("10");

        assertEquals("https://repo1.maven.org/maven2/org/eclipse/microprofile/fault-tolerance/microprofile-fault-tolerance-tck/1.0.0/microprofile-fault-tolerance-tck-1.0.0.jar",
                     microprofile.getTCKURL());
        assertEquals("https://download.eclipse.org/ee4j/data/jakartaee/promoted/eftl/data-tck-1.0.0.zip",
                     jakartaWithOutVersion.getTCKURL());
        assertEquals("https://download.eclipse.org/ee4j/dependency-injection/jakartaee10/promoted/eftl/dependency-injection-tck-1.0.0.zip",
                     jakartaWithVersion.getTCKURL());
    }

    private Method getSpecNameForURL() throws Exception {
        Method getSpecNameForURL = TCKResultsInfo.class.getDeclaredMethod("getSpecNameForURL");
        getSpecNameForURL.setAccessible(true);
        return getSpecNameForURL;
    }
}
