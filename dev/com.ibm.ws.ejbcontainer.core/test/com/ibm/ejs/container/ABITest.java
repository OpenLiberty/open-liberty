/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EnterpriseBean;
import javax.ejb.Handle;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ejs.container.finder.CollectionCannotBeFurtherAccessedException;
import com.ibm.ejs.container.finder.FinderResultClientEnumeration;
import com.ibm.ejs.persistence.EJSPersistenceException;
import com.ibm.ejs.persistence.OptimisticUpdateFailureException;
import com.ibm.websphere.cpi.Finder;
import com.ibm.websphere.cpi.Persister;
import com.ibm.websphere.cpi.PersisterHome;
import com.ibm.websphere.csi.CSIServant;

/**
 * This test verifies that methods referenced by ejbdeploy are not removed.
 * The list might be incomplete.
 */
public class ABITest
{
    private static void checkMember(Member member, boolean prot)
    {
        Assert.assertTrue(Modifier.isPublic(member.getModifiers()) || (prot && Modifier.isProtected(member.getModifiers())));
    }

    private static void checkConstructor(Class<?> klass, Class<?>[] paramTypes, boolean allowProtected)
                    throws NoSuchMethodException
    {
        checkMember(klass.getDeclaredConstructor(paramTypes), allowProtected);
    }

    private static void checkConstructor(Class<?> klass, Class<?>[] paramTypes)
                    throws NoSuchMethodException
    {
        checkConstructor(klass, paramTypes, false);
    }

    private static void checkField(Class<?> klass, String name, Class<?> type, boolean allowProtected)
                    throws NoSuchFieldException
    {
        Field field = null;
        if (allowProtected)
        {
            for (Class<?> classIter = klass; classIter != null; classIter = classIter.getSuperclass())
            {
                try
                {
                    field = classIter.getDeclaredField(name);
                    break;
                } catch (NoSuchFieldException ex) {
                }
            }
        }

        if (field == null)
        {
            field = klass.getField(name);
        }

        checkMember(field, allowProtected);
        Assert.assertEquals(type, field.getType());
    }

    private static void checkMethod(Class<?> klass, String name, Class<?>[] paramTypes, Class<?> returnType, boolean allowProtected)
                    throws NoSuchMethodException
    {
        Method method = null;
        if (allowProtected)
        {
            for (Class<?> classIter = klass; classIter != null; classIter = classIter.getSuperclass())
            {
                try
                {
                    method = classIter.getDeclaredMethod(name, paramTypes);
                    break;
                } catch (NoSuchMethodException ex) {
                }
            }
        }

        if (method == null)
        {
            method = klass.getMethod(name, paramTypes);
        }

        checkMember(method, allowProtected);
        Assert.assertEquals(returnType, method.getReturnType());
    }

    private static void checkMethod(Class<?> klass, String name, Class<?>[] paramTypes, Class<?> returnType)
                    throws NoSuchMethodException
    {
        checkMethod(klass, name, paramTypes, returnType, false);
    }

    @Test
    public void testBeanId()
                    throws Exception
    {
        checkConstructor(BeanId.class, new Class<?>[] { HomeInternal.class, Serializable.class });
    }

    @Test
    public void testBeanManagedBeanO()
                    throws Exception
    {
        checkMethod(BeanManagedBeanO.class, "getEnterpriseBean", null, EnterpriseBean.class);
    }

    @Test
    public void testBeanO()
                    throws Exception
    {
        checkMethod(BeanO.class, "getEnterpriseBean", null, EnterpriseBean.class);
    }

    @Test
    public void testCreateFailureException()
                    throws Exception
    {
        checkConstructor(CreateFailureException.class, new Class<?>[] { Throwable.class });
    }

    @Test
    public void testEJSContainer()
                    throws Exception
    {
        checkMethod(EJSContainer.class, "doesJaccNeedsEJBArguments", new Class<?>[] { EJSWrapperBase.class }, boolean.class);
        checkMethod(EJSContainer.class, "getEJSDeployedSupport", new Class<?>[] { EJSWrapperBase.class }, EJSDeployedSupport.class);
        checkMethod(EJSContainer.class, "postInvoke", new Class<?>[] { EJSWrapper.class, int.class, EJSDeployedSupport.class }, void.class);
        checkMethod(EJSContainer.class, "postInvoke", new Class<?>[] { EJSWrapperBase.class, int.class, EJSDeployedSupport.class }, void.class);
        checkMethod(EJSContainer.class, "preInvoke", new Class<?>[] { EJSWrapperBase.class, int.class, EJSDeployedSupport.class }, EnterpriseBean.class);
        checkMethod(EJSContainer.class, "preInvoke", new Class<?>[] { EJSWrapperBase.class, int.class, EJSDeployedSupport.class, Object[].class }, EnterpriseBean.class);
        checkMethod(EJSContainer.class, "putEJSDeployedSupport", new Class<?>[] { EJSDeployedSupport.class }, void.class);
    }

    @Test
    public void testEJSDeployedSupport()
                    throws Exception
    {
        checkConstructor(EJSDeployedSupport.class, null);
        checkMethod(EJSDeployedSupport.class, "setCheckedException", new Class<?>[] { Exception.class }, void.class);
        checkMethod(EJSDeployedSupport.class, "setUncheckedException", new Class<?>[] { Exception.class }, void.class);
        checkMethod(EJSDeployedSupport.class, "setUncheckedException", new Class<?>[] { Throwable.class }, void.class);
        checkMethod(EJSDeployedSupport.class, "setUncheckedLocalException", new Class<?>[] { Throwable.class }, void.class);
    }

    @Test
    public void testEJSHome()
                    throws Exception
    {
        checkConstructor(EJSHome.class, null);
        checkField(EJSHome.class, "persister", Persister.class, true);
        checkMethod(EJSHome.class, "activateBean", new Class<?>[] { Object.class }, EJBObject.class);
        checkMethod(EJSHome.class, "activateBean_Local", new Class<?>[] { Object.class }, EJBLocalObject.class);
        checkMethod(EJSHome.class, "afterPostCreate", new Class<?>[] { BeanO.class, Object.class }, void.class);
        checkMethod(EJSHome.class, "afterPostCreateCompletion", new Class<?>[] { BeanO.class }, void.class);
        checkMethod(EJSHome.class, "createBeanO", null, BeanO.class);
        checkMethod(EJSHome.class, "createFailure", new Class<?>[] { BeanO.class }, void.class);
        checkMethod(EJSHome.class, "createWrapper", new Class<?>[] { BeanId.class }, EJSWrapper.class);
        checkMethod(EJSHome.class, "createWrapper_Local", new Class<?>[] { BeanId.class }, EJSLocalWrapper.class);
        checkMethod(EJSHome.class, "discardFinderEntityBeanO", new Class<?>[] { EntityBeanO.class }, void.class);
        checkMethod(EJSHome.class, "getBean", new Class<?>[] { Object.class }, EJBObject.class);
        checkMethod(EJSHome.class, "getBean", new Class<?>[] { String.class, Object.class, Object.class }, EJBObject.class);
        checkMethod(EJSHome.class, "getBean_Local", new Class<?>[] { Object.class }, EJBLocalObject.class);
        checkMethod(EJSHome.class, "getCMP20Collection", new Class<?>[] { Collection.class }, Collection.class);
        checkMethod(EJSHome.class, "getCMP20Collection_Local", new Class<?>[] { Collection.class }, Collection.class);
        checkMethod(EJSHome.class, "getCMP20Enumeration", new Class<?>[] { Enumeration.class }, Enumeration.class);
        checkMethod(EJSHome.class, "getCMP20Enumeration_Local", new Class<?>[] { Enumeration.class }, Enumeration.class);
        checkMethod(EJSHome.class, "getCollection", new Class<?>[] { Finder.class }, Collection.class);
        checkMethod(EJSHome.class, "getCollection", new Class<?>[] { Collection.class }, Collection.class);
        checkMethod(EJSHome.class, "getEnumeration", new Class<?>[] { Finder.class }, Enumeration.class);
        checkMethod(EJSHome.class, "getEnumeration", new Class<?>[] { Enumeration.class }, Enumeration.class);
        checkMethod(EJSHome.class, "getFindByPrimaryKeyEntityBeanO", null, EntityBeanO.class);
        checkMethod(EJSHome.class, "getFinderBeanO", null, BeanManagedBeanO.class);
        checkMethod(EJSHome.class, "getFinderEntityBeanO", null, EntityBeanO.class);
        checkMethod(EJSHome.class, "postCreate", new Class<?>[] { BeanO.class }, EJBObject.class);
        checkMethod(EJSHome.class, "postCreate", new Class<?>[] { BeanO.class, Object.class }, EJBObject.class);
        checkMethod(EJSHome.class, "postCreate", new Class<?>[] { BeanO.class, Object.class, boolean.class }, EJBObject.class);
        checkMethod(EJSHome.class, "postCreate_Local", new Class<?>[] { BeanO.class }, EJBLocalObject.class);
        checkMethod(EJSHome.class, "postCreate_Local", new Class<?>[] { BeanO.class, Object.class, boolean.class }, EJBLocalObject.class);
        checkMethod(EJSHome.class, "preEjbCreate", new Class<?>[] { BeanO.class }, boolean.class);
        checkMethod(EJSHome.class, "releaseFinderBeanO", new Class<?>[] { BeanManagedBeanO.class }, void.class);
        checkMethod(EJSHome.class, "releaseFinderEntityBeanO", new Class<?>[] { EntityBeanO.class }, void.class);
    }

    @Test
    public void testEJSLocalWrapper()
                    throws Exception
    {
        checkConstructor(EJSLocalWrapper.class, null);
        checkField(EJSWrapper.class, "container", EJSContainer.class, true);
    }

    @Test
    public void testEJSRemoteWrapper()
                    throws Exception
    {
        checkMethod(EJSRemoteWrapper.class, "wlmable", null, boolean.class);
    }

    @Test
    public void testEJSWrapper()
                    throws Exception
    {
        checkConstructor(EJSWrapper.class, null);
        checkField(EJSWrapper.class, "container", EJSContainer.class, true);
        checkMethod(EJSWrapper.class, "getEJBHome", null, EJBHome.class);
        checkMethod(EJSWrapper.class, "getHandle", null, Handle.class);
        checkMethod(EJSWrapper.class, "getPrimaryKey", null, Object.class);
        checkMethod(EJSWrapper.class, "isIdentical", new Class<?>[] { EJBObject.class }, boolean.class);
        checkMethod(EJSWrapper.class, "remove", null, void.class);
    }

    @Test
    public void testEntityBeanO()
                    throws Exception
    {
        checkMethod(EntityBeanO.class, "getEnterpriseBean", null, EnterpriseBean.class);
    }

    @Test
    public void testStatelessBeanO()
                    throws Exception
    {
        checkMethod(StatelessBeanO.class, "getId", null, BeanId.class);
    }

    @Test
    public void testCollectionCannotBeFurtherAccessedException()
    {
        Assert.assertNotNull(CollectionCannotBeFurtherAccessedException.class);
    }

    //    @Test
    //    public void testFinderResultClientCollection_Local()
    //                    throws Exception
    //    {
    //        checkMethod(FinderResultClientCollection_Local.class, "getServerWrapperSize", null, int.class);
    //    }

    @Test
    public void testFinderResultClientEnumeration()
                    throws Exception
    {
        checkMethod(FinderResultClientEnumeration.class, "getCurrentWrappers", null, Vector.class);
    }

    //    @Test
    //    public void testFinderResultClientEnumeration_Local()
    //                    throws Exception
    //    {
    //        checkMethod(FinderResultClientEnumeration_Local.class, "getServerWrapperSize", null, int.class);
    //    }

    //    @Test
    //    public void testEJSFinder()
    //                    throws Exception
    //    {
    //        Assert.assertNotNull(EJSFinder.class);
    //    }

    //    @Test
    //    public void testEJSJDBCFinder()
    //                    throws Exception
    //    {
    //        checkConstructor(EJSJDBCFinder.class, new Class<?>[] { ResultSet.class, EJSJDBCPersister.class, PreparedStatement.class });
    //        checkMethod(EJSJDBCFinder.class, "close", null, void.class);
    //        checkMethod(EJSJDBCFinder.class, "hasMoreElements", null, boolean.class);
    //        checkMethod(EJSJDBCFinder.class, "nextElement", null, Object.class);
    //    }

    //    @Test
    //    public void testEJSJDBCPersister()
    //                    throws Exception
    //    {
    //        checkConstructor(EJSJDBCPersister.class, null);
    //        checkField(EJSJDBCPersister.class, "home", PersisterHome.class, true);
    //        checkMethod(EJSJDBCPersister.class, "getBean", new Class<?>[] { Object.class }, EJBObject.class);
    //        checkMethod(EJSJDBCPersister.class, "getDataFromCache", null, Object[].class);
    //        checkMethod(EJSJDBCPersister.class, "getPreparedStatement", new Class<?>[] { String.class }, PreparedStatement.class);
    //        checkMethod(EJSJDBCPersister.class, "load", new Class<?>[] { EntityBean.class, Object.class, boolean.class }, void.class);
    //        checkMethod(EJSJDBCPersister.class, "preFind", null, void.class);
    //        checkMethod(EJSJDBCPersister.class, "putDataIntoCache", new Class<?>[] { Object[].class }, void.class);
    //        checkMethod(EJSJDBCPersister.class, "returnPreparedStatement", new Class<?>[] { PreparedStatement.class }, void.class, true);
    //    }

    @Test
    public void testEJSPersistenceException()
                    throws Exception
    {
        checkConstructor(EJSPersistenceException.class, new Class<?>[] { String.class });
        checkConstructor(EJSPersistenceException.class, new Class<?>[] { String.class, Throwable.class });
    }

    @Test
    public void testOptimisticUpdateFailureException()
                    throws Exception
    {
        checkConstructor(OptimisticUpdateFailureException.class, new Class<?>[] { String.class });
    }

    @Test
    public void testPersisterHome()
                    throws Exception
    {
        checkMethod(PersisterHome.class, "activateBean", new Class<?>[] { Object.class }, EJBObject.class);
        checkMethod(PersisterHome.class, "getBean", new Class<?>[] { Object.class }, EJBObject.class);
    }

    @Test
    public void testCSIServant()
                    throws Exception
    {
        checkMethod(CSIServant.class, "wlmable", null, boolean.class);
    }
}
