/*******************************************************************************
 * Copyright (c) 2011,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.basic.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 *
 */
public class BasicRegistryTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private BasicRegistry fullBasicRegistry() {

//        Set<BasicUser> users = new HashSet<BasicUser>();
//        users.add(new BasicUser("notGrouped", "nopwd"));
//        users.add(new BasicUser("user1", "pass1"));
//        users.add(new BasicUser("user 2", "pass 2"));
//        users.add(new BasicUser("CN=dnUser", "pwd"));
//
//        Set<BasicGroup> groups = new HashSet<BasicGroup>();
//        groups.add(new BasicGroup("group0", new HashSet<String>()));
//        Set<String> members = new HashSet<String>();
//        members.add("user1");
//        groups.add(new BasicGroup("group1", members));
//        members = new HashSet<String>();
//        members.add("user1");
//        members.add("user 2");
//        groups.add(new BasicGroup("my group 2", members));
//        groups.add(new BasicGroup("myGroupWithNoUsers", new HashSet<String>()));
        BasicRegistryConfig brc = new BasicRegistryConfig() {

            @Override
            public String realm() {
                return "myRealm";
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public User[] user() {
                return new User[] {
                                    new User() {

                                        @Override
                                        public String name() {
                                            return "notGrouped";
                                        }

                                        @Override
                                        public SerializableProtectedString password() {
                                            return new SerializableProtectedString("nopwd".toCharArray());
                                        }
                                    },
                                    new User() {

                                        @Override
                                        public String name() {
                                            return "user1";
                                        }

                                        @Override
                                        public SerializableProtectedString password() {
                                            return new SerializableProtectedString("pass1".toCharArray());
                                        }
                                    },
                                    new User() {

                                        @Override
                                        public String name() {
                                            return "user 2";
                                        }

                                        @Override
                                        public SerializableProtectedString password() {
                                            return new SerializableProtectedString("pass 2".toCharArray());
                                        }
                                    },
                                    new User() {

                                        @Override
                                        public String name() {
                                            return "CN=dnUser";
                                        }

                                        @Override
                                        public SerializableProtectedString password() {
                                            return new SerializableProtectedString("pwd".toCharArray());
                                        }
                                    }
                };
            }

            @Override
            public Group[] group() {
                return new Group[] {
                                     new Group() {

                                         @Override
                                         public String name() {
                                             return "group0";
                                         }

                                         @Override
                                         public Member[] member() {
                                             return new Member[] {};
                                         }
                                     },
                                     new Group() {

                                         @Override
                                         public String name() {
                                             return "group1";
                                         }

                                         @Override
                                         public Member[] member() {
                                             return new Member[] {
                                                                   new Member() {

                                                                       @Override
                                                                       public String name() {
                                                                           return "user1";
                                                                       }
                                                                   }
                                             };
                                         }
                                     },
                                     new Group() {

                                         @Override
                                         public String name() {
                                             return "my group 2";
                                         }

                                         @Override
                                         public Member[] member() {
                                             return new Member[] {
                                                                   new Member() {

                                                                       @Override
                                                                       public String name() {
                                                                           return "user1";
                                                                       }
                                                                   },
                                                                   new Member() {

                                                                       @Override
                                                                       public String name() {
                                                                           return "user 2";
                                                                       }
                                                                   }
                                             };
                                         }
                                     },
                                     new Group() {

                                         @Override
                                         public String name() {
                                             return "myGroupWithNoUsers";
                                         }

                                         @Override
                                         public Member[] member() {
                                             return new Member[] {};
                                         }
                                     }
                };
            }

            @Override
            public String config_id() {
                return "configId";
            }

            @Override
            public String certificateMapMode() {
                return null;
            }

            @Override
            public String certificateMapperId() {
                return null;
            }

            @Override
            public String CertificateMapper_target() {
                return null;
            }
        };
        BasicRegistry reg = new BasicRegistry();
        reg.activate(brc);
        return reg;
    }

    private BasicRegistry emptyBasicRegistry() {
        BasicRegistryConfig brc = new BasicRegistryConfig() {

            @Override
            public String realm() {
                return null;
            }

            @Override
            public boolean ignoreCaseForAuthentication() {
                return false;
            }

            @Override
            public User[] user() {
                return new User[] {};
            }

            @Override
            public Group[] group() {
                return new Group[] {};
            }

            @Override
            public String config_id() {
                return "empty";
            }

            @Override
            public String certificateMapMode() {
                return null;
            }

            @Override
            public String certificateMapperId() {
                return null;
            }

            @Override
            public String CertificateMapper_target() {
                return null;
            }
        };
        BasicRegistry reg = new BasicRegistry();
        reg.activate(brc);
        return reg;
    }

    private BasicRegistry ignoreCaseBasicRegistry() {

//        Set<BasicUser> users = new HashSet<BasicUser>();
//        users.add(new BasicUser("user1", "pass1"));
//        users.add(new BasicUser("USER2", "PASS2"));
        BasicRegistryConfig brc = new BasicRegistryConfig() {
            @Override
            public String realm() {
                return "ignoreCase";
            }
            @Override
            public boolean ignoreCaseForAuthentication() {
                return true;
            }
            @Override
            public User[] user() {
                return new User[] {
                                    new User() {
                                        @Override
                                        public String name() {
                                            return "user1";
                                        }
                                        @Override
                                        public SerializableProtectedString password() {
                                            return new SerializableProtectedString("pass1".toCharArray());
                                        }
                                    },
                                    new User() {
                                        @Override
                                        public String name() {
                                            return "USER2";
                                        }
                                        @Override
                                        public SerializableProtectedString password() {
                                            return new SerializableProtectedString("pass2".toCharArray());
                                        }
                                    }
                };
            }
            @Override
            public Group[] group() {
                return new Group[] {};
            }
            @Override
            public String config_id() {
                return "configId";
            }
            @Override
            public String certificateMapMode() {
                return null;
            }
            @Override
            public String certificateMapperId() {
                return null;
            }
            @Override
            public String CertificateMapper_target() {
                return null;
            }
        };
        BasicRegistry reg = new BasicRegistry();
        reg.activate(brc);
        return reg;
    }

    /**
     * Constructor can support a null realm value.
     */
//    @Test
//    public void ctor_realmCanBeNull() throws Exception {
//        new BasicRegistry(null, Boolean.FALSE, new HashSet<BasicUser>(), new HashSet<BasicGroup>());
//    }

    /**
     * Constructor does not support a null users value.
     */
//    @Test(expected = NullPointerException.class)
//    public void ctor_usersCanNotBeNull() throws Exception {
//        new BasicRegistry("realm", Boolean.FALSE, null, new HashSet<BasicGroup>());
//    }

    /**
     * Constructor does not support a null groups value.
     */
//    @Test(expected = NullPointerException.class)
//    public void ctor_groupsNotBeNull() throws Exception {
//        new BasicRegistry("realm", Boolean.FALSE, new HashSet<BasicUser>(), null);
//    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getRealm()}.
     * getRealm() shall return the default realm name in-lieu of a
     * configured value.
     */
    //realm is required in metatype
//    @Test
//    public void getRealm_default() throws Exception {
//        UserRegistry reg = emptyBasicRegistry();
//        assertEquals(BasicRegistry.DEFAULT_REALM_NAME, fullBasicRegistry().getRealm());
//    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getRealm()}.
     * getRealm() shall return the configured realm name.
     */
    @Test
    public void getRealm_configured() throws Exception {
        assertEquals("myRealm", fullBasicRegistry().getRealm());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#checkPassword(String, String)}.
     * checkPassword() shall return null if the specified username/password
     * is not valid.
     */
    @Test
    public void checkPassword_invalidCredentials() throws Exception {
        assertNull(fullBasicRegistry().checkPassword("user1", "badPassword"));
        assertNull(fullBasicRegistry().checkPassword("badUser", "irrelevantPassword"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#checkPassword(String, String)}.
     * checkPassword() shall return true if the default username/password
     * is provided for a default configuration.
     */
    @Test
    public void checkPassword_validCredentials() throws Exception {
        assertEquals("user1", fullBasicRegistry().checkPassword("user1", "pass1"));
        assertEquals("user 2", fullBasicRegistry().checkPassword("user 2", "pass 2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#checkPassword(String, String)}.
     * checkPassword() shall throw an exception when empty.
     */
    @Test(expected = IllegalArgumentException.class)
    public void checkPassword_emptyBlanks() throws Exception {
        fullBasicRegistry().checkPassword("user", " ");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_userDoesntExist() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        final X500Principal principal = new X500Principal("cn=iDontExist");
        mock.checking(new Expectations() {
            {
                allowing(cert).getSubjectX500Principal();
                will(returnValue(principal));
            }
        });
        fullBasicRegistry().mapCertificate(new X509Certificate[] { cert });
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test(expected = CertificateMapFailedException.class)
    public void mapCertificate_noCN() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        final X500Principal principal = new X500Principal("o=nocn");
        mock.checking(new Expectations() {
            {
                allowing(cert).getSubjectX500Principal();
                will(returnValue(principal));
            }
        });
        fullBasicRegistry().mapCertificate(new X509Certificate[] { cert });
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#mapCertificate(java.security.cert.X509Certificate)}.
     */
    @Test
    public void mapCertificate() throws Exception {
        final X509Certificate cert = mock.mock(X509Certificate.class);
        final X500Principal principal = new X500Principal("CN=user1");
        mock.checking(new Expectations() {
            {
                allowing(cert).getSubjectX500Principal();
                will(returnValue(principal));
            }
        });
        assertEquals("user1", fullBasicRegistry().mapCertificate(new X509Certificate[] { cert }));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#isValidUser(String)}.
     */
    @Test
    public void isValidUser() throws Exception {
        BasicRegistry reg = fullBasicRegistry();
        assertFalse(reg.isValidUser("user0"));
        assertTrue(reg.isValidUser("user1"));
        assertTrue(reg.isValidUser("user 2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_emptyList() throws Exception {
        UserRegistry reg = emptyBasicRegistry();
        SearchResult result = reg.getUsers("*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     * A negative limit results in a default SearchResult object.
     */
    @Test
    public void getUsers_negativeLimit() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("*", -1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_noResultBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("abc*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_noResultUnbounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("abc*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_oneResultOverBound() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user1*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_oneResultBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user1*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_oneResultUnbound() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user1*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultUnderBound() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user.*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertTrue(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultOverBound() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user.*", 3);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(2, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user.*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(2, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_multipleResultUnbounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("user.*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(2, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresUnderBound() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers(".*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertTrue(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresOverBound() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers(".*", 5);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(4, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers(".*", 4);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals("Should only be 4 entries", 4, result.getList().size());
        assertFalse("Should not think there are more results", result.hasMore());
        assertTrue("Should contain notGrouped", result.getList().contains("notGrouped"));
        assertTrue("Should contain user1", result.getList().contains("user1"));
        assertTrue("Should contain user 2", result.getList().contains("user 2"));
        assertTrue("Should contain CN=dnUser", result.getList().contains("CN=dnUser"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_allEntiresUnbounded() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers(".*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals("Should only be 4 entries", 4, result.getList().size());
        assertFalse("Should not think there are more results", result.hasMore());
        assertTrue("Should contain notGrouped", result.getList().contains("notGrouped"));
        assertTrue("Should contain user1", result.getList().contains("user1"));
        assertTrue("Should contain user 2", result.getList().contains("user 2"));
        assertTrue("Should contain CN=dnUser", result.getList().contains("CN=dnUser"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_noResultCaseMismatch() throws Exception {
        SearchResult result = fullBasicRegistry().getUsers("USER.*", 3);
        assertNotNull("SearchResult must not be NULL", result);
        assertEquals(0, result.getList().size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsers(String, int)}.
     */
    @Test
    public void getUsers_ignoreCaseUnbounded() throws Exception {
        SearchResult result = ignoreCaseBasicRegistry().getUsers("user.*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals("Should be 2 entries", 2, result.getList().size());
        assertFalse("Should not think there are more results", result.hasMore());
        assertTrue("Should contain user1", result.getList().contains("user1"));
        assertTrue("Should contain USER2", result.getList().contains("USER2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUserDisplayName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserDisplayName_doesNotExist() throws Exception {
        fullBasicRegistry().getUserDisplayName("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUserDisplayName(String)}.
     */
    @Test
    public void getUserDisplayName_exists() throws Exception {
        assertEquals("user1", fullBasicRegistry().getUserDisplayName("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueUserId(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueUserId_doesNotExist() throws Exception {
        fullBasicRegistry().getUniqueUserId("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueUserId(String)}.
     */
    @Test
    public void getUniqueUserId_exists() throws Exception {
        assertEquals("user1", fullBasicRegistry().getUniqueUserId("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUserSecurityName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUserSecurityName_doesNotExist() throws Exception {
        fullBasicRegistry().getUserSecurityName("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUserSecurityName(String)}.
     */
    @Test
    public void getUserSecurityName_exists() throws Exception {
        assertEquals("user1", fullBasicRegistry().getUserSecurityName("user1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#isValidGroup(String)}.
     */
    @Test
    public void isValidGroup() throws Exception {
        BasicRegistry reg = fullBasicRegistry();
        assertTrue(reg.isValidGroup("group1"));
        assertTrue(reg.isValidGroup("my group 2"));
        assertFalse(reg.isValidGroup("group3"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     * A negative limit results in a default SearchResult object.
     */
    @Test
    public void getGroups_emptyList() throws Exception {
        UserRegistry reg = emptyBasicRegistry();
        SearchResult result = reg.getGroups("*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     * A negative limit results in a default SearchResult object.
     */
    @Test
    public void getGroups_negativeLimit() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("*", -1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_noResultBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("abc*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_noResultUnbounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("abc*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(0, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_oneResultOverBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("group1*", 2);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_oneResultBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("group1*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_oneResultUnbounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("group1*", 0);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_multipleResultUnderBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("group.*", 1);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(1, result.getList().size());
        assertTrue(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_multipleResultBonded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups("group.*", 3);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals(2, result.getList().size());
        assertFalse(result.hasMore());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroups(String, int)}.
     */
    @Test
    public void getGroups_allEntiresBounded() throws Exception {
        SearchResult result = fullBasicRegistry().getGroups(".*", 4);
        assertNotNull("SearchResult must never be NULL", result);
        assertEquals("Should only be 4 entries", 4, result.getList().size());
        assertFalse("Should not think there are more results", result.hasMore());
        assertTrue("Should contain group0", result.getList().contains("group0"));
        assertTrue("Should contain group1", result.getList().contains("group1"));
        assertTrue("Should contain 'my group 2'", result.getList().contains("my group 2"));
        assertTrue("Should contain myGroupWithNoUsers", result.getList().contains("myGroupWithNoUsers"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupDisplayName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupDisplayName_doesNotExist() throws Exception {
        fullBasicRegistry().getGroupDisplayName("group9");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupDisplayName(String)}.
     */
    @Test
    public void getGroupDisplayName_exists() throws Exception {
        assertEquals("group1", fullBasicRegistry().getGroupDisplayName("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupId(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupId_doesNotExist() throws Exception {
        fullBasicRegistry().getUniqueGroupId("group9");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupId(String)}.
     */
    @Test
    public void getUniqueGroupId_exists() throws Exception {
        assertEquals("group1", fullBasicRegistry().getUniqueGroupId("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupSecurityName(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupSecurityName_doesNotExist() throws Exception {
        fullBasicRegistry().getGroupSecurityName("group9");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupSecurityName(String)}.
     */
    @Test
    public void getGroupSecurityName_exists() throws Exception {
        assertEquals("group1", fullBasicRegistry().getGroupSecurityName("group1"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupIdsForUser(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUniqueGroupIdsForUser_noSuchUser() throws Exception {
        fullBasicRegistry().getUniqueGroupIdsForUser("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupIdsForUser(String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_userWithNoGroups() throws Exception {
        List<String> groups = fullBasicRegistry().getUniqueGroupIdsForUser("notGrouped");
        assertNotNull(groups);
        assertEquals(0, groups.size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupIdsForUser(String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_singleGroup() throws Exception {
        List<String> groups = fullBasicRegistry().getUniqueGroupIdsForUser("user 2");
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertTrue(groups.contains("my group 2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUniqueGroupIdsForUser(String)}.
     */
    @Test
    public void getUniqueGroupIdsForUser_multipleGroups() throws Exception {
        List<String> groups = fullBasicRegistry().getUniqueGroupIdsForUser("user1");
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertTrue(groups.contains("group1"));
        assertTrue(groups.contains("my group 2"));
    }

    /**
     * Test method for (@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsersForGroup(String, int)).
     */
    @Test(expected = EntryNotFoundException.class)
    public void getUsersForGroup_noSuchGroup() throws Exception {
        fullBasicRegistry().getUsersForGroup("invalidGroup", 0);
    }

    /**
     * Test method for (@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsersForGroup(String, int)).
     */
    @Test
    public void getUsersForGroup_groupWithNoUsers() throws Exception {
        SearchResult result = fullBasicRegistry().getUsersForGroup("myGroupWithNoUsers", 0);
        List<String> members = result.getList();
        assertTrue(members.isEmpty());
    }

    /**
     * Test method for (@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsersForGroup(String, int)).
     */
    @Test
    public void getUsersForGroup_groupWithOneUser() throws Exception {
        SearchResult result = fullBasicRegistry().getUsersForGroup("group1", 0);
        List<String> members = result.getList();
        assertNotNull(members);
        assertEquals(1, members.size());
        assertEquals("user1", members.get(0));

    }

    /**
     * Test method for (@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getUsersForGroup(String, int)).
     */
    @Test
    public void getUsersForGroup_groupWithMultipleUser() throws Exception {
        SearchResult result = fullBasicRegistry().getUsersForGroup("my group 2", 0);
        List<String> members = result.getList();
        assertNotNull(members);
        assertEquals(2, members.size());
        assertTrue(members.contains("user 2"));
        assertTrue(members.contains("user1"));

    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupsForUser(String)}.
     */
    @Test(expected = EntryNotFoundException.class)
    public void getGroupsForUser_noSuchUser() throws Exception {
        fullBasicRegistry().getGroupsForUser("user0");
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupsForUser(String)}.
     */
    @Test
    public void getGroupsForUser_userWithNoGroups() throws Exception {
        List<String> groups = fullBasicRegistry().getGroupsForUser("notGrouped");
        assertNotNull(groups);
        assertEquals(0, groups.size());
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupsForUser(String)}.
     */
    @Test
    public void getGroupsForUser_singleGroup() throws Exception {
        List<String> groups = fullBasicRegistry().getGroupsForUser("user 2");
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertTrue(groups.contains("my group 2"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.basic.internal.BasicRegistry#getGroupsForUser(String)}.
     */
    @Test
    public void getGroupsForUser_multipleGroups() throws Exception {
        List<String> groups = fullBasicRegistry().getGroupsForUser("user1");
        assertNotNull(groups);
        assertEquals(2, groups.size());
        assertTrue(groups.contains("group1"));
        assertTrue(groups.contains("my group 2"));
    }

}
