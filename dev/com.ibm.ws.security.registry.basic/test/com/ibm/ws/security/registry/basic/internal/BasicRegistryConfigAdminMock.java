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
package com.ibm.ws.security.registry.basic.internal;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;

import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * Common setup for the unit tests that need to mock up the ConfigAdmin.
 * Since this has gotten complicated, its easier to centralize all of this.
 */
@SuppressWarnings("unchecked")
public abstract class BasicRegistryConfigAdminMock {
    protected final Mockery mock = new JUnit4Mockery();
//    protected final ComponentContext cc = mock.mock(ComponentContext.class);
//    protected final BundleContext bundleContext = mock.mock(BundleContext.class);
//    protected final ServiceRegistration clReg = mock.mock(ServiceRegistration.class);
//
//    protected final ServiceReference<ConfigurationAdmin> configAdminRef = mock.mock(ServiceReference.class);
//    protected final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class);
//
//    protected final Configuration configUserInstance0 = mock.mock(Configuration.class, "user_instance0");
//    protected final Configuration configUserInstance1 = mock.mock(Configuration.class, "user_instance1");
//    protected final Configuration configUserInstance2 = mock.mock(Configuration.class, "user_instance2");
//    protected final Configuration configUserInstance3 = mock.mock(Configuration.class, "user_instance3");
    protected final User user0 = new User() {

        @Override
        public String name() {
            return "encodedUser";
        }

        @Override
        public SerializableProtectedString password() {
            return new SerializableProtectedString("{xor}Lz4sLGw=".toCharArray()); // pass3
        }
    };
    protected final User user1 = new User() {

        @Override
        public String name() {
            return "user1";
        }

        @Override
        public SerializableProtectedString password() {
            return new SerializableProtectedString("pass1".toCharArray());
        }
    };
    protected final User user2 = new User() {

        @Override
        public String name() {
            return " user 2 ";
        }

        @Override
        public SerializableProtectedString password() {
            return new SerializableProtectedString(" pass 2 ".toCharArray());
        }
    };
    protected final User user3 = new User() {

        @Override
        public String name() {
            return "hashedUser";
        }

        @Override
        public SerializableProtectedString password() {
            return new SerializableProtectedString("{hash}ATAAAAAIrWG9DLi4GG1AAAAAICxa4vuxiH0IY8WDt8S3thPwPwtpClRjGehZmzI2Jczr".toCharArray()); // password
        }
    };

//    protected final Configuration configGroupInstance0 = mock.mock(Configuration.class, "group_instance0");
//    protected final Configuration configGroupInstance1 = mock.mock(Configuration.class, "group_instance1");
//    protected final Configuration configGroupInstance2 = mock.mock(Configuration.class, "group_instance2");
//    protected final Configuration configGroupInstance3 = mock.mock(Configuration.class, "group_instance3");

    protected final Group group0 = new Group() {

        @Override
        public String name() {
            return "group0";
        }

        @Override
        public Member[] member() {
            return new Member[] {};
        }
    };
    protected final Group group1 = new Group() {

        @Override
        public String name() {
            return "group1";
        }

        @Override
        public Member[] member() {
            return new Member[] { member1 };
        }
    };
    protected final Group group2 = new Group() {

        @Override
        public String name() {
            return " my group 2 ";
        }

        @Override
        public Member[] member() {
            return new Member[] { member1, member2 };
        }
    };
    protected final Group group3 = new Group() {

        @Override
        public String name() {
            return "multiGroup";
        }

        @Override
        public Member[] member() {
            return new Member[] { member2, member2 };
        }
    };

//    protected final Configuration configMemberInstance1 = mock.mock(Configuration.class, "member_instance1");
//    protected final Configuration configMemberInstance2 = mock.mock(Configuration.class, "member_instance2");
    protected final Member member1 = new Member() {

        @Override
        public String name() {
            return "user1";
        }
    };
    protected final Member member2 = new Member() {

        @Override
        public String name() {
            return " user 2 ";
        }
    };

    /**
     * Perform some setup to simulate the service starting.
     *
     * Builds a basicRegistry that would look like this: {@literal <basicRegistry> <user name="encodedUser" password=" xor}Lz4sLGw="/> //user_instance0
     * <user name="user1" password="pass1"/> //user_instance1
     * <user name=" user 2 " password=" pass 2 "/> //user_instance2
     * <group name="group0" /> //group_instance0
     * <group name="group1"> //group_instance1
     * <member name="user1" />
     * </group>
     * <group name="my group 2" /> //group_instance2
     * <member name="user1" />
     * <member name="user2" />
     * </group>
     * <group name="multiGroup" /> //group_instance3
     * <member name="user2" />
     * <member name="user2" />
     * </group>
     * }
     * </basicRegistry>
     */
//    public void setUp() throws Exception {
//        final Dictionary configUserInstance0Props = new Hashtable();
//        configUserInstance0Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "encodedUser");
//        configUserInstance0Props.put(DynamicBasicRegistry.CFG_KEY_PASSWORD, "{xor}Lz4sLGw="); // pass3
//
//        final Dictionary configUserInstance1Props = new Hashtable();
//        configUserInstance1Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "user1");
//        configUserInstance1Props.put(DynamicBasicRegistry.CFG_KEY_PASSWORD, "pass1");
//
//        final Dictionary configUserInstance2Props = new Hashtable();
//        configUserInstance2Props.put(DynamicBasicRegistry.CFG_KEY_NAME, " user 2 ");
//        configUserInstance2Props.put(DynamicBasicRegistry.CFG_KEY_PASSWORD, " pass 2 ");
//
//        final Dictionary configUserInstance3Props = new Hashtable();
//        configUserInstance3Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "hashedUser");
//        configUserInstance3Props.put(DynamicBasicRegistry.CFG_KEY_PASSWORD, "{hash}ATAAAAAIrWG9DLi4GG1AAAAAICxa4vuxiH0IY8WDt8S3thPwPwtpClRjGehZmzI2Jczr"); // password
//
//        final Dictionary configGroupInstance0Props = new Hashtable();
//        configGroupInstance0Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "group0");
//
//        final Dictionary configGroupInstance1Props = new Hashtable();
//        configGroupInstance1Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "group1");
//        configGroupInstance1Props.put(DynamicBasicRegistry.CFG_KEY_MEMBER, new String[] { "member_instance1" });
//
//        final Dictionary configGroupInstance2Props = new Hashtable();
//        configGroupInstance2Props.put(DynamicBasicRegistry.CFG_KEY_NAME, " my group 2 ");
//        configGroupInstance2Props.put(DynamicBasicRegistry.CFG_KEY_MEMBER, new String[] { "member_instance1", "member_instance2" });
//
//        final Dictionary configGroupInstance3Props = new Hashtable();
//        configGroupInstance3Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "multiGroup");
//        configGroupInstance3Props.put(DynamicBasicRegistry.CFG_KEY_MEMBER, new String[] { "member_instance2", "member_instance2" });
//
//        final Dictionary configMemberInstance1Props = new Hashtable();
//        configMemberInstance1Props.put(DynamicBasicRegistry.CFG_KEY_NAME, "user1");
//
//        final Dictionary configMemberInstance2Props = new Hashtable();
//        configMemberInstance2Props.put(DynamicBasicRegistry.CFG_KEY_NAME, " user 2 ");
//
//        mock.checking(new Expectations() {
//            {
//                allowing(cc).getBundleContext();
//                will(returnValue(bundleContext));
//                allowing(bundleContext).registerService(with(any(Class.class)), with(any(ConfigurationListener.class)), with(any(Dictionary.class)));
//                will(returnValue(clReg));
//
//                allowing(cc).locateService(BasicRegistryFactory.KEY_CONFIG_ADMIN, configAdminRef);
//                will(returnValue(configAdmin));
//
//                allowing(configAdmin).getConfiguration("user_instance0");
//                will(returnValue(configUserInstance0));
//                allowing(configUserInstance0).getProperties();
//                will(returnValue(configUserInstance0Props));
//
//                allowing(configAdmin).getConfiguration("user_instance1");
//                will(returnValue(configUserInstance1));
//                allowing(configUserInstance1).getProperties();
//                will(returnValue(configUserInstance1Props));
//
//                allowing(configAdmin).getConfiguration("user_instance2");
//                will(returnValue(configUserInstance2));
//                allowing(configUserInstance2).getProperties();
//                will(returnValue(configUserInstance2Props));
//
//                allowing(configAdmin).getConfiguration("user_instance3");
//                will(returnValue(configUserInstance3));
//                allowing(configUserInstance3).getProperties();
//                will(returnValue(configUserInstance3Props));
//
//                allowing(configAdmin).getConfiguration("group_instance0");
//                will(returnValue(configGroupInstance0));
//                allowing(configGroupInstance0).getProperties();
//                will(returnValue(configGroupInstance0Props));
//
//                allowing(configAdmin).getConfiguration("group_instance1");
//                will(returnValue(configGroupInstance1));
//                allowing(configGroupInstance1).getProperties();
//                will(returnValue(configGroupInstance1Props));
//
//                allowing(configAdmin).getConfiguration("group_instance2");
//                will(returnValue(configGroupInstance2));
//                allowing(configGroupInstance2).getProperties();
//                will(returnValue(configGroupInstance2Props));
//
//                allowing(configAdmin).getConfiguration("group_instance3");
//                will(returnValue(configGroupInstance3));
//                allowing(configGroupInstance3).getProperties();
//                will(returnValue(configGroupInstance3Props));
//
//                allowing(configAdmin).getConfiguration("member_instance1");
//                will(returnValue(configMemberInstance1));
//                allowing(configMemberInstance1).getProperties();
//                will(returnValue(configMemberInstance1Props));
//
//                allowing(configAdmin).getConfiguration("member_instance2");
//                will(returnValue(configMemberInstance2));
//                allowing(configMemberInstance2).getProperties();
//                will(returnValue(configMemberInstance2Props));
//
//                allowing(configAdmin).getConfiguration("invalidInstance");
//                will(returnValue(null));
//
//            }
//        });
//
//    }

}