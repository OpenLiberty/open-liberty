/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.testlogic;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Query;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;

public abstract class AbstractTestLogic {
    // Can switch the value of this constant to enable validation of @preUpdate
    // on exception paths if EclipseLink is ever updated to support this behavior.
    public static final boolean ECLIPSELINK_VALIDATE_PREUPDATE_ON_EXCEPTION = false;

    public final static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    public final static ObjectName fatServerInfoMBeanObjectName;

    static {
        ObjectName on = null;
        try {
            on = new ObjectName("WebSphereFAT:name=ServerInfo");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            fatServerInfoMBeanObjectName = on;
        }
    }

    protected Class<?> resolveEntityClass(JPAEntityClassEnum enumerationRef) throws ClassNotFoundException {
        if (enumerationRef == null) {
            throw new IllegalArgumentException("AbstractTestLogic.resolveEntityClass: argument cannot be null.");
        }

        String className = enumerationRef.getEntityClassName();
        if (className == null || "".equals(className)) {
            throw new IllegalArgumentException("AbstractTestLogic.resolveEntityClass: className cannot be null.");
        }

        ClassLoader ctxClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }

        });

        return Class.forName(className, true, ctxClassLoader);
    }

    protected Object constructNewEntityObject(JPAEntityClassEnum enumerationRef) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> classType = resolveEntityClass(enumerationRef);
        Class<?> constructorArgSig[] = new Class[] {};
        Object constructorArgs[] = new Object[] {};

        Constructor<?> classConstructor = classType
                        .getConstructor(constructorArgSig);
        Object newEntity = classConstructor.newInstance(constructorArgs);

        return newEntity;
    }

    protected Object constructNewEntityObject(Class<?> entityClass) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> constructorArgSig[] = new Class[] {};
        Object constructorArgs[] = new Object[] {};

        Constructor<?> classConstructor = entityClass
                        .getConstructor(constructorArgSig);
        Object newEntity = classConstructor.newInstance(constructorArgs);

        return newEntity;
    }

    /**
     * Cleans the database tables for the entities specified by the array of
     * enumerations. The enumerations provided in the array must have the method
     * "getEntityName" with no parameters. This method must return a String
     * containing the entity named to be used in the query to select all
     * instances of the entity.
     *
     * @param em
     * @param tj
     * @param testEntityEnumArr
     * @param log
     */
    protected final void cleanupDatabase(EntityManager em, TransactionJacket tj, JPAEntityClassEnum testEntityEnumArr[]) {
        if (testEntityEnumArr == null || testEntityEnumArr.length == 0) {
            return;
        }

        try {
            System.out.println("Cleaning database up...");

            // Begin Transaction
            if (tj.isTransactionActive()) {
                System.out.println("Cleanup routine is rolling back currently active transaction.");
                tj.rollbackTransaction();
            }
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                em.joinTransaction();
            }

            // Clear persistence context
            em.clear();

            // Set the default flush mode to COMMIT, as some persistence providers may try flush
            // between queries and cause foreign key constraint violations.
            em.setFlushMode(FlushModeType.COMMIT);

            // Iterate through all of the enumerations in the array provided by the caller.
            // Consumable enumerations MUST define the method "public String getEntityName()".
            for (int index = 0; index < testEntityEnumArr.length; index++) {
                String entityName = testEntityEnumArr[index].getEntityName();
                System.out.println("Removing all instances of " + entityName + " from database...");

                String queryStr = "SELECT e FROM " + entityName + " e";
                Query query = em.createQuery(queryStr);
                List<?> resultList = query.getResultList();

                // Skip if resultList is null
                if (resultList == null) {
                    continue;
                }

                // Iterate through each entity object in the resultList. We don't care what class type the object is,
                // we know that it's an entity, and we want to remove it, so we iterate through every object in the list
                // and em.remove() it.
                Iterator<?> i = resultList.iterator();
                while (i.hasNext()) {
                    Object entityObj = i.next();
                    em.remove(entityObj);
                }
            }

            // Commit transaction
            tj.commitTransaction();
        } catch (Throwable t) {
            throw t;
        } finally {
            if (tj.isTransactionActive())
                tj.rollbackTransaction();
        }
    }

    /**
     * Cleans the database tables for the entities specified by the array of enumerations. The enumerations provided
     * in the array must have the method "getEntityName" with no parameters. This method must return a String
     * containing the entity named to be used in the query to select all instances of the entity.
     *
     * This is a convenience method that uses List instead of an array of JPAEntityClassEnum objects.
     *
     * @param em
     * @param tj
     * @param testEntityEnumArr
     * @param log
     */
    protected final void cleanupDatabase(
                                         EntityManager em, // EntityManager
                                         TransactionJacket tj, // Wrapper for EntityTransaction/JTA User Tran
                                         List<JPAEntityClassEnum> testEntityEnumList) { // Array of entity enumerations that are to be cleared from the database
        JPAEntityClassEnum[] testEntityEnumArr = (JPAEntityClassEnum[]) testEntityEnumList.toArray();
        cleanupDatabase(em, tj, testEntityEnumArr);
    }

    public enum JPAProviderImpl {
        OPENJPA,
        ECLIPSELINK,
        HIBERNATE;
    }

    public JPAProviderImpl getJPAProviderImpl(EntityManager em) {
        if (em == null) {
            return null;
        }

        String delegateClassStr = em.getDelegate().getClass().getName();
        if (delegateClassStr == null) {
            return null;
        }

        if (delegateClassStr.toLowerCase().contains("openjpa")) {
            return JPAProviderImpl.OPENJPA;
        }

        if (delegateClassStr.toLowerCase().contains("com.ibm")) {
            return JPAProviderImpl.OPENJPA;
        }

        if (delegateClassStr.toLowerCase().contains("eclipse")) {
            return JPAProviderImpl.ECLIPSELINK;
        }

        if (delegateClassStr.toLowerCase().contains("hibernate")) {
            return JPAProviderImpl.HIBERNATE;
        }

        return null;
    }

    public JPAProviderImpl getJPAProviderImpl(JPAResource jpaRsc) {
        if (jpaRsc == null) {
            return null;
        }

        return getJPAProviderImpl(jpaRsc.getEm());
    }

    protected String toBeanMethod(String prefix, String fieldName) {
        return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1, fieldName.length());
    }

    @SuppressWarnings("unused")
    protected void beginTx(JPAResource jpaRW) {
        System.out.println("Begin a Tx");
        jpaRW.getTj().beginTransaction();
        if (jpaRW.getTj().isApplicationManaged()) {
            System.out.println("Joining entitymanager to JTA transaction...");
            jpaRW.getEm().joinTransaction();
        }
    }

    @SuppressWarnings("unused")
    protected void commitTx(JPAResource jpaRW) {
        System.out.println("Commit current Tx");
        jpaRW.getTj().commitTransaction();
    }

    @SuppressWarnings("unused")
    protected void rollbackTx(JPAResource jpaRW) {
        System.out.println("Rollback current Tx");
        jpaRW.getTj().rollbackTransaction();
    }

    protected void assertExceptionIsInChain(final Class exceptionClass, Throwable t) {
        Assert.assertThat("Assert " + exceptionClass.getName() + "  is in Exception chain.",
                          t,
                          getExceptionChainMatcher(exceptionClass));
    }

    protected Matcher getExceptionChainMatcher(final Class t) {
        return new BaseMatcher() {
            final protected Class<?> expectedThrowableClass = t;

            @Override
            public boolean matches(Object obj) {
                if (obj == null) {
                    return (expectedThrowableClass == null);
                }

                if (!(obj instanceof Throwable)) {
                    return false;
                }

                final ArrayList<Throwable> tList = new ArrayList<Throwable>();

                Throwable t = (Throwable) obj;
                while (t != null) {
                    tList.add(t);
                    if (expectedThrowableClass.equals(t.getClass())) {
                        return true;
                    }

                    if (expectedThrowableClass.isAssignableFrom(t.getClass())) {
                        return true;
                    }

                    t = t.getCause();
                }

                StringBuilder sb = new StringBuilder();
                sb.append("getExceptionChainMatcher: looking for ");
                sb.append(expectedThrowableClass);
                sb.append(" but only found: ");
                boolean first = true;
                for (Throwable te : tList) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(" -> ");
                    }
                    sb.append(te.getClass().getName());
                }

                System.out.println(sb);

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expectedThrowableClass.toString());
            }

        };
    }

    protected String getTestName() {
        final StackTraceElement[] steArr = Thread.currentThread().getStackTrace();

        String cn = null;
        for (int index = 0; index < steArr.length; index++) {
            final String name = steArr[index].getClassName();
            if (name.startsWith("java.") || name.startsWith("javax.")) {
                continue;
            }
            cn = name;
            break;
        }

        String methodName = cn.substring(cn.lastIndexOf('.'));

        return this.getClass().getSimpleName() + "." + methodName;
    }

    // Basing determination off product version using
    // info from https://www.ibm.com/support/knowledgecenter/en/SSEPEK_11.0.0/java/src/tpc/imjcc_c0053013.html
    protected boolean isDB2ForZOS(String prodVersion) throws Exception {
        if (prodVersion != null && prodVersion.toLowerCase().startsWith("dsn")) {
            return true;
        }

        return false;
    }

    protected boolean isDB2ForLUW(String prodVersion) throws Exception {
        if (prodVersion != null && prodVersion.toLowerCase().startsWith("sql")) {
            return true;
        }

        return false;
    }

    protected boolean isDB2ForISeries(String prodVersion) throws Exception {
        if (prodVersion != null && prodVersion.toLowerCase().startsWith("qsq")) {
            return true;
        }

        return false;
    }

    protected boolean isDB2ForVM_VSE(String prodVersion) throws Exception {
        if (prodVersion != null && prodVersion.toLowerCase().startsWith("ari")) {
            return true;
        }

        return false;
    }

    protected boolean isDB2(String prodVersion) throws Exception {
        return isDB2ForLUW(prodVersion) || isDB2ForZOS(prodVersion) || isDB2ForISeries(prodVersion);
    }

    protected boolean isDerby(String lDbProductName) throws Exception {
        return (lDbProductName == null) ? false : lDbProductName.contains("derby");
    }

    protected Set<String> getInstalledFeatures() {
        HashSet<String> retVal = new HashSet<String>();

        try {
            Set<String> instFeatureSet = (Set<String>) mbeanServer.getAttribute(fatServerInfoMBeanObjectName, "InstalledFeatures");
            if (instFeatureSet != null) {
                retVal.addAll(instFeatureSet);
            }
        } catch (Throwable t) {
        }
        return retVal;
    }

    protected boolean isUsingJPA20Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.0");
    }

    protected boolean isUsingJPA21Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.1");
    }

    protected boolean isUsingJPA22Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.2");
    }

    protected boolean isUsingJPA21ContainerFeature(boolean onlyContainerFeature) {
        Set<String> instFeatureSet = getInstalledFeatures();
        if (onlyContainerFeature && instFeatureSet.contains("jpa-2.1"))
            return false;
        return instFeatureSet.contains("jpaContainer-2.1");
    }

    protected boolean isUsingJPA22ContainerFeature(boolean onlyContainerFeature) {
        Set<String> instFeatureSet = getInstalledFeatures();
        if (onlyContainerFeature && instFeatureSet.contains("jpa-2.2"))
            return false;
        return instFeatureSet.contains("jpaContainer-2.2");
    }
}
