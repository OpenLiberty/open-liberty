/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.config;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;

import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 *
 */
public class PasswordTypeTest extends ManagedFactoryTest {

    /**
     * @param name
     */
    public PasswordTypeTest(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    public void configurationUpdated(String pid, Dictionary properties) throws Exception {

        String id = (String) properties.get("id");

        if (id.equals("p1")) {

            // <test.config.password id="p1" name="admin" password="secret,secret"/>
            assertEquals("p1.name", "admin", properties.get("name"));
            SerializableProtectedString sps = (SerializableProtectedString) properties.get("password");
            assertEquals("p1.password", "secret,secret", new String(sps.getChars()));

        } else if (id.equals("p2")) {

            // <test.config.password id="p2" name="my//name/" password="secret//secret/"/>
            // Only substitution variables are path-normalized. p2 contains no substitution 
            // variables, therefore none of the attribute values are path-normalized.

            assertEquals("p2.name", "my//name/", properties.get("name"));
            SerializableProtectedString sps = (SerializableProtectedString) properties.get("password");
            assertEquals("p2.password", "secret//secret/", new String(sps.getChars()));

        } else if (id.equals("p3")) {

            // <test.config.password id="p3" name="${p3.name}" password="${p3.password}"/>
            // p3.name=my//name/
            // p3.password=secret//secret/

            // p3 contains substitution variables. Substitution variables are path-normalized for all 
            // attribute types EXCEPT ibm:type="password".  This test ensures that the password attribute
            // value is not path-normalized.

            // Path-normalization will...
            //     1. convert sequences of "//" to just "/"
            //     2. strip off any trailing "/" from the value.

            // NOTE: We are (at least temporarily) removing path normalization for variables, so the test below is
            // changing to expect that "my//name/" will not be normalized.            
            assertEquals("p3.name", "my//name/", properties.get("name"));

            // "secret//secret/" ==> "secret//secret/"  (no normalization)
            SerializableProtectedString sps = (SerializableProtectedString) properties.get("password");
            assertEquals("p2.password", "secret//secret/", new String(sps.getChars()));

        } else {
            throw new RuntimeException("Invalid test.config.password instance id: " + id);
        }

    }

}
