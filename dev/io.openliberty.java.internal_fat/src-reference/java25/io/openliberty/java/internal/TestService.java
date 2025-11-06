/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.java.internal;

import module java.base;            // Module Import Declarations : JEP 511 -> https://openjdk.org/jeps/511
//import java.io.PrintWriter;       Covered by "import module java.base"
//import java.io.StringWriter;      Covered by "import module java.base"
//import java.util.ArrayList;       Covered by "import module java.base"
//import java.util.Arrays;          Covered by "import module java.base"
//import java.util.List;            Covered by "import module java.base"
//import javax.crypto.KDF;          Covered by "import module java.base"
//import javax.crypto.SecretKey;    Covered by "import module java.base"
//import javax.crypto.spec.HKDFParameterSpec;        Covered by "import module java.base"
//import java.security.spec.AlgorithmParameterSpec;  Covered by "import module java.base"
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
@ApplicationScoped
public class TestService {

    private StringWriter sw = new StringWriter();
    
    private static final ScopedValue<String> NAME = ScopedValue.newInstance();  // Scoped Values : JEP 506 -> https://openjdk.org/jeps/506

    @GET
    public String test() {
        try {
            log(">>> ENTER");
            doTest();
            log("<<< EXIT SUCCESSFUL");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            e.printStackTrace(new PrintWriter(sw));
            log("<<< EXIT FAILED");
        }
        String result = sw.toString();
        sw = new StringWriter();
        return result;
    }


    private void doTest() throws Exception {
        log("Beginning Java 25 testing");

        Vehicle van = new Car("2015", "Toyota", "Sienna");
        if (!"Silver".equals(van.color)) {
                throw new Exception ("Wrong car color for the Sienna!");
        } else {
            log("Right color car found for the Sienna!");
        }
        
        van = new Car("2019", "Honda", "Odyssey");
        if (!"Yellow".equals(van.color)) {
                throw new Exception ("Wrong car color for the Odyssey!");
        } else {
            log("Right color car found for the Odyssey!");
        }
        
        ScopedValue.where(NAME, "Dave").run(() -> Greeting());
        ScopedValue.where(NAME, "Jared").run(() -> Greeting());
        
        CreateKeyDerivationFunction();
        
        log("Leaving testing");
    }

    private class Vehicle {
        String color;
        
        protected Vehicle(String year, String make, String model) {
                if ("2015".equals(year) && "Toyota".equals(make) && "Sienna".equals(model)) {
                        color = "Silver";
                } else {
                        color = "Yellow";
                }
        }
    }

    private class Car extends Vehicle {
        String year;
        String make;
        String model;
        
        protected Car(String year, String make, String model) {
                if (year == null || make == null || model == null) {
                        throw new IllegalArgumentException("Constructor information passed in is invalid");
                }
                
                super(year, make, model);               // Flexible Constructor Bodies : JEP 513 -> https://openjdk.org/jeps/513
        }
    }
    
    // Compact Source Files and Instance Main Methods : JEP 512 -> https://openjdk.org/jeps/512
    void main() {
        IO.println("Hello World!");
    }
    
    // Scoped Values : JEP 506 -> https://openjdk.org/jeps/506
    private void Greeting() {
        log("Hello " + NAME.get() + "!");
    }
    
    // Key Derivation Function API : JEP 510 -> https://openjdk.org/jeps/510
    private void CreateKeyDerivationFunction() throws Exception {
        
        byte[] salt = {0x00, 0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F};
        byte[] ikm = {'A','C','E','G','I','K','M','O','Q','S','U','W','Y'};
    
        // initialization of an HKDF-Extract AlgorithmParameterSpec
        AlgorithmParameterSpec derivationSpec =
             HKDFParameterSpec.ofExtract()
                              .addIKM(ikm)
                              .addSalt(salt).thenExpand(null, 64);
    
        try {
            KDF keyDerivation = KDF.getInstance("HKDF-SHA256");    // Create a KDF object for HKDF-SHA256
            SecretKey sKey = keyDerivation.deriveKey("AES", derivationSpec);    // Derive a 64-byte AES key
        } catch (Exception e) {
            throw e;
        }
    }
    
    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }
}
