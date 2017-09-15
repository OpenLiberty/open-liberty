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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test a complex config requirement that was requested from the security team
 */
public class NestedConfigComplexElementTest extends ManagedFactoryTest {

    private final ConfigurationAdmin configAdmin;
    private final Map realms, ids, passwords, users;
    private final String GROUP_ELEM = "group";
    private final String USER_ELEM = "user";
    private final String ID_ATTR = "id";
    private final String PASSWORD_ATTR = "password";
    private final String REALM_ATTR = "realm";

    /**
     * @param name
     */
    public NestedConfigComplexElementTest(String name, ConfigurationAdmin configAdmin) {
        super(name);
        this.configAdmin = configAdmin;
        realms = new HashMap();
        ids = new HashMap();
        passwords = new HashMap();
        users = new HashMap();
        init();
    }

    /**
     * results data to compare against
     */
    private void init() {
        realms.put("SampleBasicRealm", null);
        ids.put("admin", "SampleBasicRealm");
        ids.put("user1", "SampleBasicRealm");
        ids.put("user2", "SampleBasicRealm");
        ids.put("user3", "SampleBasicRealm");
        ids.put("memberlessGroup", "SampleBasicRealm");
        ids.put("adminGroup", "SampleBasicRealm");
        ids.put("users", "SampleBasicRealm");
        passwords.put("admin", "admin");
        passwords.put("user1", "user1");
        passwords.put("user2", "user2");
        passwords.put("user3", "user3");
        users.put("admin", "SampleBasicRealm");
        users.put("user1", "SampleBasicRealm");
        users.put("user2", "SampleBasicRealm");
    }

    /** {@inheritDoc} */
    @Override
    public void configurationUpdated(String pid, Dictionary properties) throws Exception {
        String name = (String) properties.get(REALM_ATTR);
        assertTrue(realms.containsKey(name));
        String[] userArr = (String[]) properties.get(USER_ELEM);
        assertNotNull(userArr);
        assertEquals(4, userArr.length);
        String[] groupArr = (String[]) properties.get(GROUP_ELEM);
        assertNotNull(groupArr);
        assertEquals(3, groupArr.length);

        for (int i = 0; i < userArr.length; i++) {
            Configuration config = configAdmin.getConfiguration(userArr[i]);
            Dictionary prop = config.getProperties();
            String id = (String) prop.get(ID_ATTR);
            assertTrue(ids.containsKey(id));
            assertEquals(ids.get(id), name);
            String password = (String) prop.get(PASSWORD_ATTR);
            assertTrue(passwords.containsKey(password));
            assertEquals(passwords.get(password), id);
        }

        for (int i = 0; i < groupArr.length; i++) {
            Configuration config = configAdmin.getConfiguration(groupArr[i]);
            Dictionary prop = config.getProperties();
            String id = (String) prop.get(ID_ATTR);
            assertTrue(ids.containsKey(id));
            assertEquals(ids.get(id), name);
            String[] userList = (String[]) prop.get(USER_ELEM);
            if (userList != null) {
                for (int j = 0; j < userList.length; j++) {
                    assertTrue(users.containsKey(userList[j]));
                    assertEquals(users.get(userList[j]), name);
                }
            }
        }

    }
}
