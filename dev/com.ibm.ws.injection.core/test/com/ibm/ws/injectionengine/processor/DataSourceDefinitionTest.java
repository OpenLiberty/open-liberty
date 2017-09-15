/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.injectionengine.TestHelper;
import com.ibm.ws.injectionengine.TestInjectionEngineImpl;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;

public class DataSourceDefinitionTest
{
    public static class DataSourceImpl
                    implements DataSource
    {
        private final String ivName;
        private final String ivClassName;
        private String ivDescription;
        private String ivServerName;
        private Integer ivPortNumber;
        private String ivDatabaseName;
        private String ivUrl;
        private String ivUser;
        private String ivPassword;
        private final List<Property> ivProperties = new ArrayList<Property>();
        private Integer ivLoginTimeout;
        private Boolean ivTransactional;
        private int ivIsolationLevel;
        private Integer ivInitialPoolSize;
        private Integer ivMaxPoolSize;
        private Integer ivMinPoolSize;
        private Integer ivMaxIdleTime;
        private Integer ivMaxStatements;

        DataSourceImpl(String name, String className)
        {
            ivName = name;
            ivClassName = className;
        }

        @Override
        public String getName()
        {
            return ivName;
        }

        @Override
        public String getClassNameValue()
        {
            return ivClassName;
        }

        public DataSourceImpl setDescription(String description)
        {
            ivDescription = description;
            return this;
        }

        @Override
        public String getDescription()
        {
            return ivDescription;
        }

        public DataSourceImpl setServerName(String serverName)
        {
            ivServerName = serverName;
            return this;
        }

        @Override
        public String getServerName()
        {
            return ivServerName;
        }

        public DataSourceImpl setPortNumber(int portNumber)
        {
            ivPortNumber = portNumber;
            return this;
        }

        @Override
        public boolean isSetPortNumber()
        {
            return ivPortNumber != null;
        }

        @Override
        public int getPortNumber()
        {
            return ivPortNumber;
        }

        public DataSourceImpl setDatabaseName(String databaseName)
        {
            ivDatabaseName = databaseName;
            return this;
        }

        @Override
        public String getDatabaseName()
        {
            return ivDatabaseName;
        }

        public DataSourceImpl setUrl(String url)
        {
            ivUrl = url;
            return this;
        }

        @Override
        public String getUrl()
        {
            return ivUrl;
        }

        public DataSourceImpl setUser(String user)
        {
            ivUser = user;
            return this;
        }

        @Override
        public String getUser()
        {
            return ivUser;
        }

        public DataSourceImpl setPassword(String password)
        {
            ivPassword = password;
            return this;
        }

        @Override
        public String getPassword()
        {
            return ivPassword;
        }

        public DataSourceImpl setLoginTimeout(int loginTimeout)
        {
            ivLoginTimeout = loginTimeout;
            return this;
        }

        public DataSourceImpl addProperty(final String name, final String value)
        {
            ivProperties.add(new Property()
            {
                @Override
                public String getName()
                {
                    return name;
                }

                @Override
                public String getValue()
                {
                    return value;
                }
            });
            return this;
        }

        @Override
        public List<Property> getProperties()
        {
            return ivProperties;
        }

        @Override
        public boolean isSetLoginTimeout()
        {
            return ivLoginTimeout != null;
        }

        @Override
        public int getLoginTimeout()
        {
            return ivLoginTimeout;
        }

        public DataSourceImpl setTransactional(boolean transactional)
        {
            ivTransactional = transactional;
            return this;
        }

        @Override
        public boolean isSetTransactional()
        {
            return ivTransactional != null;
        }

        @Override
        public boolean isTransactional()
        {
            return ivTransactional;
        }

        public DataSourceImpl setIsolationLevel(int isolationLevel)
        {
            ivIsolationLevel = isolationLevel;
            return this;
        }

        @Override
        public int getIsolationLevelValue()
        {
            return ivIsolationLevel;
        }

        public DataSourceImpl setInitialPoolSize(int initialPoolSize)
        {
            ivInitialPoolSize = initialPoolSize;
            return this;
        }

        @Override
        public boolean isSetInitialPoolSize()
        {
            return ivInitialPoolSize != null;
        }

        @Override
        public int getInitialPoolSize()
        {
            return ivInitialPoolSize;
        }

        public DataSourceImpl setMaxPoolSize(int maxPoolSize)
        {
            ivMaxPoolSize = maxPoolSize;
            return this;
        }

        @Override
        public boolean isSetMaxPoolSize()
        {
            return ivMaxPoolSize != null;
        }

        @Override
        public int getMaxPoolSize()
        {
            return ivMaxPoolSize;
        }

        public DataSourceImpl setMinPoolSize(int minPoolSize)
        {
            ivMinPoolSize = minPoolSize;
            return this;
        }

        @Override
        public boolean isSetMinPoolSize()
        {
            return ivMinPoolSize != null;
        }

        @Override
        public int getMinPoolSize()
        {
            return ivMinPoolSize;
        }

        public DataSourceImpl setMaxIdleTime(int maxIdleTime)
        {
            ivMaxIdleTime = maxIdleTime;
            return this;
        }

        @Override
        public boolean isSetMaxIdleTime()
        {
            return ivMaxIdleTime != null;
        }

        @Override
        public int getMaxIdleTime()
        {
            return ivMaxIdleTime;
        }

        public DataSourceImpl setMaxStatements(int maxStatements)
        {
            ivMaxStatements = maxStatements;
            return this;
        }

        @Override
        public boolean isSetMaxStatements()
        {
            return ivMaxStatements != null;
        }

        @Override
        public int getMaxStatements()
        {
            return ivMaxStatements;
        }
    }

    @Test
    public void testDataSourceDefinition()
                    throws Exception
    {
        TestHelper helper = new TestHelper()
                        .addDataSourceDefinition(new DataSourceImpl("xmldefault", "com.ibm.DSDClass"))
                        .addDataSourceDefinition(new DataSourceImpl("xmldefaultbinding", "com.ibm.DSDClass"))
                        .addDataSourceDefinition(new DataSourceImpl("xmlexplicit", "com.ibm.DSDClass")
                                        .setDescription("testDescription")
                                        .setServerName("testServer")
                                        .setPortNumber(1234)
                                        .setDatabaseName("testDatabase")
                                        .setUrl("test://")
                                        .setUser("testUser")
                                        .setPassword("testPassword")
                                        .setLoginTimeout(2345)
                                        .setTransactional(false)
                                        .setIsolationLevel(Connection.TRANSACTION_SERIALIZABLE)
                                        .setInitialPoolSize(3456)
                                        .setMinPoolSize(4567)
                                        .setMaxPoolSize(5678)
                                        .setMaxIdleTime(6789)
                                        .setMaxStatements(7890)
                                        .addProperty("name1", "value1")
                                        .addProperty("name2", "value2"))
                        .addDataSourceDefinition(new DataSourceImpl("xmlexplicitbinding", "com.ibm.DSDClass")
                                        .setDescription("testDescription")
                                        .setServerName("testServer")
                                        .setPortNumber(1234)
                                        .setDatabaseName("testDatabase")
                                        .setUrl("test://")
                                        .setUser("testUser")
                                        .setPassword("testPassword")
                                        .setLoginTimeout(2345)
                                        .setTransactional(false)
                                        .setIsolationLevel(Connection.TRANSACTION_SERIALIZABLE)
                                        .setInitialPoolSize(3456)
                                        .setMinPoolSize(4567)
                                        .setMaxPoolSize(5678)
                                        .setMaxIdleTime(6789)
                                        .setMaxStatements(7890)
                                        .addProperty("name1", "value1")
                                        .addProperty("name2", "value2"))
                        .addDataSourceDefinitionBinding("anndefaultbinding", "binding")
                        .addDataSourceDefinitionBinding("annexplicitbinding", "binding")
                        .addDataSourceDefinitionBinding("xmldefaultbinding", "binding")
                        .addDataSourceDefinitionBinding("xmlexplicitbinding", "binding")
                        .setJavaColonCompEnvMap()
                        .addInjectionClass(TestDataSourceDefinition.class)
                        .process();
        Map<String, InjectionBinding<?>> bindings = helper.getJavaColonCompEnvMap();

        for (String name : new String[] { "anndefault", "anndefaultbinding", "xmldefault", "xmldefaultbinding" })
        {
            TestInjectionEngineImpl.DefinitionReference ref = (TestInjectionEngineImpl.DefinitionReference) bindings.get(name).getBindingObject();
            Assert.assertEquals(javax.sql.DataSource.class.getName(), ref.getClassName());
            Assert.assertNull(ref.ivScope);
            Assert.assertEquals(name, ref.ivJndiName);
            Assert.assertEquals("com.ibm.DSDClass", ref.ivProperties.get("className"));
            Assert.assertNull(ref.ivProperties.get("description"));
            Assert.assertNull(ref.ivProperties.get("serverName"));
            Assert.assertNull(ref.ivProperties.get("portNumber"));
            Assert.assertNull(ref.ivProperties.get("databaseName"));
            Assert.assertNull(ref.ivProperties.get("url"));
            Assert.assertNull(ref.ivProperties.get("user"));
            Assert.assertNull(ref.ivProperties.get("password"));
            Assert.assertNull(ref.ivProperties.get("loginTimeout"));
            Assert.assertNull(ref.ivProperties.get("transactional"));
            Assert.assertNull(ref.ivProperties.get("isolationLevel"));
            Assert.assertNull(ref.ivProperties.get("initialPoolSize"));
            Assert.assertNull(ref.ivProperties.get("minPoolSize"));
            Assert.assertNull(ref.ivProperties.get("maxPoolSize"));
            Assert.assertNull(ref.ivProperties.get("maxIdleTime"));
            Assert.assertNull(ref.ivProperties.get("maxStatements"));
            String bindingName = name.endsWith("binding") ? "binding" : null;
            Assert.assertEquals(bindingName, ref.ivBindingName);
        }

        for (String name : new String[] { "annexplicit", "annexplicitbinding", "xmlexplicit", "xmlexplicitbinding" })
        {
            TestInjectionEngineImpl.DefinitionReference ref = (TestInjectionEngineImpl.DefinitionReference) bindings.get(name).getBindingObject();
            Assert.assertEquals(javax.sql.DataSource.class.getName(), ref.getClassName());
            Assert.assertNull(ref.ivScope);
            Assert.assertEquals(name, ref.ivJndiName);
            Assert.assertEquals("com.ibm.DSDClass", ref.ivProperties.get("className"));
            Assert.assertEquals("testDescription", ref.ivProperties.get("description"));
            Assert.assertEquals("testServer", ref.ivProperties.get("serverName"));
            Assert.assertEquals(1234, ref.ivProperties.get("portNumber"));
            Assert.assertEquals("testDatabase", ref.ivProperties.get("databaseName"));
            Assert.assertEquals("test://", ref.ivProperties.get("url"));
            Assert.assertEquals("testUser", ref.ivProperties.get("user"));
            Assert.assertEquals("testPassword", ref.ivProperties.get("password"));
            Assert.assertEquals(2345, ref.ivProperties.get("loginTimeout"));
            Assert.assertEquals(false, ref.ivProperties.get("transactional"));
            Assert.assertEquals(Connection.TRANSACTION_SERIALIZABLE, ref.ivProperties.get("isolationLevel"));
            Assert.assertEquals(3456, ref.ivProperties.get("initialPoolSize"));
            Assert.assertEquals(4567, ref.ivProperties.get("minPoolSize"));
            Assert.assertEquals(5678, ref.ivProperties.get("maxPoolSize"));
            Assert.assertEquals(6789, ref.ivProperties.get("maxIdleTime"));
            Assert.assertEquals(7890, ref.ivProperties.get("maxStatements"));
            Assert.assertEquals("value1", ref.ivProperties.get("name1"));
            Assert.assertEquals("value2", ref.ivProperties.get("name2"));
            String bindingName = name.endsWith("binding") ? "binding" : null;
            Assert.assertEquals(bindingName, ref.ivBindingName);
        }
    }

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "anndefault", className = "com.ibm.DSDClass"),
                            @DataSourceDefinition(name = "anndefaultbinding", className = "com.ibm.DSDClass"),
                            @DataSourceDefinition(
                                                  name = "annexplicit",
                                                  className = "com.ibm.DSDClass",
                                                  description = "testDescription",
                                                  serverName = "testServer",
                                                  portNumber = 1234,
                                                  databaseName = "testDatabase",
                                                  url = "test://",
                                                  user = "testUser",
                                                  password = "testPassword",
                                                  loginTimeout = 2345,
                                                  transactional = false,
                                                  isolationLevel = Connection.TRANSACTION_SERIALIZABLE,
                                                  initialPoolSize = 3456,
                                                  minPoolSize = 4567,
                                                  maxPoolSize = 5678,
                                                  maxIdleTime = 6789,
                                                  maxStatements = 7890,
                                                  properties = {
                                                                "name1=value1",
                                                                "name2=value2"
                                                  }
                            ),
                            @DataSourceDefinition(
                                                  name = "annexplicitbinding",
                                                  className = "com.ibm.DSDClass",
                                                  description = "testDescription",
                                                  serverName = "testServer",
                                                  portNumber = 1234,
                                                  databaseName = "testDatabase",
                                                  url = "test://",
                                                  user = "testUser",
                                                  password = "testPassword",
                                                  loginTimeout = 2345,
                                                  transactional = false,
                                                  isolationLevel = Connection.TRANSACTION_SERIALIZABLE,
                                                  initialPoolSize = 3456,
                                                  minPoolSize = 4567,
                                                  maxPoolSize = 5678,
                                                  maxIdleTime = 6789,
                                                  maxStatements = 7890,
                                                  properties = {
                                                                "name1=value1",
                                                                "name2=value2"
                                                  }
                            )
    })
    public static class TestDataSourceDefinition { /* empty */}

    private static void checkProcessError(String elementName, boolean conflict, TestHelper helper)
                    throws InjectionException
    {
        try
        {
            helper.process();
            Assert.fail("expected " + elementName + " error");
        } catch (InjectionConfigurationException ex)
        {
            // Plural annotation processing nests exceptions.
            if (ex.getCause() instanceof InjectionConfigurationException)
            {
                ex = (InjectionConfigurationException) ex.getCause();
            }

            String message = ex.getMessage();
            Assert.assertTrue(message, message.contains(elementName));
            Assert.assertTrue(message.contains("aaa") || message.contains("111") || message.contains("false") || message.contains("TRANSACTION_READ_UNCOMMITTED"));
            if (conflict)
            {
                Assert.assertTrue(message.contains("bbb") || message.contains("222") || message.contains("true") || message.contains("TRANSACTION_READ_COMMITTED"));
            }
        }
    }

    private static void checkError(String elementName, String name, boolean conflict, TestHelper helper)
                    throws Exception
    {
        checkProcessError(elementName, conflict, helper);

        // Binding should prevent errors.
        helper
                        .setJavaColonCompEnvMap()
                        .addDataSourceDefinitionBinding(name, "binding")
                        .process();
        Map<String, InjectionBinding<?>> bindings = helper.getJavaColonCompEnvMap();

        TestInjectionEngineImpl.DefinitionReference ref = (TestInjectionEngineImpl.DefinitionReference) bindings.get(name).getBindingObject();
        if (name.equals("name0"))
        {
            Assert.assertEquals("server", ref.ivProperties.get("serverName"));
        }
        else
        {
            Assert.assertEquals("class", ref.ivProperties.get("className"));
        }

        // Binding should not prevent errors if checking app config.
        helper.setCheckApplicationConfiguration(true);
        checkProcessError(elementName, conflict, helper);
    }

    @Test
    public void testAnnPropertyError()
                    throws Exception
    {
        checkError("properties", "name", false,
                   new TestHelper()
                                   .addInjectionClass(TestAnnPropertyError.class));
    }

    @DataSourceDefinition(name = "name", className = "class", properties = "aaa")
    public static class TestAnnPropertyError { /* empty */}

    @Test
    public void testAnnIsolationLevelError()
                    throws Exception
    {
        checkError("isolationLevel", "name", false,
                   new TestHelper()
                                   .addInjectionClass(TestAnnIsolationLevelError.class));
    }

    @DataSourceDefinition(name = "name", className = "class", isolationLevel = 111)
    public static class TestAnnIsolationLevelError { /* empty */}

    private void checkXMLConflict(String elementName, DataSourceImpl ds1, DataSourceImpl ds2)
                    throws Exception
    {
        checkError(elementName, ds1.getName(), true,
                   new TestHelper()
                                   .addDataSourceDefinition(ds1)
                                   .addDataSourceDefinition(ds2));
    }

    @Test
    public void testXMLConflict()
                    throws Exception
    {
        checkXMLConflict("class-name",
                         new DataSourceImpl("name0", "aaa").setServerName("server"),
                         new DataSourceImpl("name0", "bbb").setServerName("server"));
        checkXMLConflict("description",
                         new DataSourceImpl("name", "class").setDescription("aaa"),
                         new DataSourceImpl("name", "class").setDescription("bbb"));
        checkXMLConflict("server-name",
                         new DataSourceImpl("name", "class").setServerName("aaa"),
                         new DataSourceImpl("name", "class").setServerName("bbb"));
        checkXMLConflict("port-number",
                         new DataSourceImpl("name", "class").setPortNumber(111),
                         new DataSourceImpl("name", "class").setPortNumber(222));
        checkXMLConflict("database-name",
                         new DataSourceImpl("name", "class").setDatabaseName("aaa"),
                         new DataSourceImpl("name", "class").setDatabaseName("bbb"));
        checkXMLConflict("url",
                         new DataSourceImpl("name", "class").setUrl("aaa"),
                         new DataSourceImpl("name", "class").setUrl("bbb"));
        checkXMLConflict("user",
                         new DataSourceImpl("name", "class").setUser("aaa"),
                         new DataSourceImpl("name", "class").setUser("bbb"));
        checkXMLConflict("password",
                         new DataSourceImpl("name", "class").setPassword("aaa"),
                         new DataSourceImpl("name", "class").setPassword("bbb"));
        checkXMLConflict("login-timeout",
                         new DataSourceImpl("name", "class").setLoginTimeout(111),
                         new DataSourceImpl("name", "class").setLoginTimeout(222));
        checkXMLConflict("transactional",
                         new DataSourceImpl("name", "class").setTransactional(false),
                         new DataSourceImpl("name", "class").setTransactional(true));
        checkXMLConflict("isolation-level",
                         new DataSourceImpl("name", "class").setIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED),
                         new DataSourceImpl("name", "class").setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED));
        checkXMLConflict("initial-pool-size",
                         new DataSourceImpl("name", "class").setInitialPoolSize(111),
                         new DataSourceImpl("name", "class").setInitialPoolSize(222));
        checkXMLConflict("max-pool-size",
                         new DataSourceImpl("name", "class").setMaxPoolSize(111),
                         new DataSourceImpl("name", "class").setMaxPoolSize(222));
        checkXMLConflict("min-pool-size",
                         new DataSourceImpl("name", "class").setMinPoolSize(111),
                         new DataSourceImpl("name", "class").setMinPoolSize(222));
        checkXMLConflict("max-idle-time",
                         new DataSourceImpl("name", "class").setMaxIdleTime(111),
                         new DataSourceImpl("name", "class").setMaxIdleTime(222));
        checkXMLConflict("max-statements",
                         new DataSourceImpl("name", "class").setMaxStatements(111),
                         new DataSourceImpl("name", "class").setMaxStatements(222));
        checkXMLConflict("xyz property",
                         new DataSourceImpl("name", "class").addProperty("xyz", "aaa"),
                         new DataSourceImpl("name", "class").addProperty("xyz", "bbb"));
    }

    private void checkAnnConflict(String elementName, String name, Class<?> klass)
                    throws Exception
    {
        checkError(elementName, name, true,
                   new TestHelper().addInjectionClass(klass));
    }

    @Test
    public void testAnnConflict()
                    throws Exception
    {
        checkAnnConflict("className", "name0", TestAnnConflictClassName.class);
        checkAnnConflict("serverName", "name", TestAnnConflictServerName.class);
        checkAnnConflict("portNumber", "name", TestAnnConflictPortNumber.class);
        checkAnnConflict("databaseName", "name", TestAnnConflictDatabaseName.class);
        checkAnnConflict("url", "name", TestAnnConflictURL.class);
        checkAnnConflict("user", "name", TestAnnConflictUser.class);
        checkAnnConflict("password", "name", TestAnnConflictPassword.class);
        checkAnnConflict("loginTimeout", "name", TestAnnConflictLoginTimeout.class);
        // transactional (boolean) cannot be a merge error.
        checkAnnConflict("isolationLevel", "name", TestAnnConflictIsolationLevel.class);
        checkAnnConflict("initialPoolSize", "name", TestAnnConflictInitialPoolSize.class);
        checkAnnConflict("maxPoolSize", "name", TestAnnConflictMaxPoolSize.class);
        checkAnnConflict("minPoolSize", "name", TestAnnConflictMinPoolSize.class);
        checkAnnConflict("maxIdleTime", "name", TestAnnConflictMaxIdleTime.class);
        checkAnnConflict("maxStatements", "name", TestAnnConflictMaxStatements.class);
        checkAnnConflict("xyz property", "name", TestAnnConflictProperties.class);
    }

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name0", className = "aaa", serverName = "server"),
                            @DataSourceDefinition(name = "name0", className = "bbb", serverName = "server")
    })
    public class TestAnnConflictClassName { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", serverName = "aaa"),
                            @DataSourceDefinition(name = "name", className = "class", serverName = "bbb")
    })
    public class TestAnnConflictServerName { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", portNumber = 111),
                            @DataSourceDefinition(name = "name", className = "class", portNumber = 222)
    })
    public class TestAnnConflictPortNumber { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", databaseName = "aaa"),
                            @DataSourceDefinition(name = "name", className = "class", databaseName = "bbb")
    })
    public class TestAnnConflictDatabaseName { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", url = "aaa"),
                            @DataSourceDefinition(name = "name", className = "class", url = "bbb")
    })
    public class TestAnnConflictURL { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", user = "aaa"),
                            @DataSourceDefinition(name = "name", className = "class", user = "bbb")
    })
    public class TestAnnConflictUser { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", password = "aaa"),
                            @DataSourceDefinition(name = "name", className = "class", password = "bbb")
    })
    public class TestAnnConflictPassword { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", loginTimeout = 111),
                            @DataSourceDefinition(name = "name", className = "class", loginTimeout = 222)
    })
    public class TestAnnConflictLoginTimeout { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", isolationLevel = Connection.TRANSACTION_READ_UNCOMMITTED),
                            @DataSourceDefinition(name = "name", className = "class", isolationLevel = Connection.TRANSACTION_READ_COMMITTED)
    })
    public class TestAnnConflictIsolationLevel { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", initialPoolSize = 111),
                            @DataSourceDefinition(name = "name", className = "class", initialPoolSize = 222)
    })
    public class TestAnnConflictInitialPoolSize { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", maxPoolSize = 111),
                            @DataSourceDefinition(name = "name", className = "class", maxPoolSize = 222)
    })
    public class TestAnnConflictMaxPoolSize { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", minPoolSize = 111),
                            @DataSourceDefinition(name = "name", className = "class", minPoolSize = 222)
    })
    public class TestAnnConflictMinPoolSize { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", maxIdleTime = 111),
                            @DataSourceDefinition(name = "name", className = "class", maxIdleTime = 222)
    })
    public class TestAnnConflictMaxIdleTime { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", maxStatements = 111),
                            @DataSourceDefinition(name = "name", className = "class", maxStatements = 222)
    })
    public class TestAnnConflictMaxStatements { /* empty */}

    @DataSourceDefinitions({
                            @DataSourceDefinition(name = "name", className = "class", properties = "xyz=aaa"),
                            @DataSourceDefinition(name = "name", className = "class", properties = "xyz=bbb")
    })
    public class TestAnnConflictProperties { /* empty */}
}
