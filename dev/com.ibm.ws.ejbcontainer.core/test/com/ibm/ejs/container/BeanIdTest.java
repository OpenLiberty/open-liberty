/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.util.ByteArray;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.csi.StatefulSessionKey;
import com.ibm.websphere.csi.StatefulSessionKeyFactory;
import com.ibm.ws.container.service.metadata.internal.J2EENameFactoryImpl;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.metadata.ejb.ModuleInitData;

public class BeanIdTest
{
    private final List<BeanMetaData> ivHomesAdded = new ArrayList<BeanMetaData>();

    @After
    public void tearDown()
    {
        for (BeanMetaData bmd : ivHomesAdded)
        {
            EJSContainer.homeOfHomes.removeHome(bmd);
        }
    }

    private void addHome(BeanMetaData bmd)
                    throws RemoteException
    {
        EJSContainer.homeOfHomes.addHome(bmd);
        ivHomesAdded.add(bmd);
    }

    /**
     * Test method for {@link com.ibm.ejs.container.BeanId#BeanId()}.
     * Verifies that the default constructor does not set a primary
     * key, J2EEName or home.
     */
    @Test
    public void testDefaultCtor()
    {
        BeanId bid = new BeanId();
        Assert.assertNull(bid.getPrimaryKey());
        Assert.assertNull(bid.getJ2EEName());
        Assert.assertFalse(bid.isHome());
    }

    /**
     * Test method for {@link com.ibm.ejs.container.BeanId#getJ2EEName()}.
     * Verifies that a J2EEName is the concatenation of app name, module
     * name and component name, with interposed '#' characters
     */
    @Test
    public void testGetJ2EEName()
    {
        J2EEName j2eeName = new J2EENameImpl("__homeOfHomes", "__homeOfHomes", "__homeOfHomes");
        J2EEName pk = new J2EENameImpl("testApp", "testModule", "testComponent");
        BeanId bid = new BeanId(j2eeName, pk, true);

        Assert.assertEquals(pk, bid.getJ2EEName());
        Assert.assertEquals(pk, bid.getPrimaryKey());
        Assert.assertTrue(bid.isHome());
    }

    @Test
    public void testEquals()
    {
        Assert.assertEquals(new BeanId(new J2EENameImpl("a", "b", "c"), null, false),
                            new BeanId(new J2EENameImpl("a", "b", "c"), null, false));
        Assert.assertTrue(new BeanId(new J2EENameImpl("a", "b", "c"), null, false).equals(
                        new BeanId(new J2EENameImpl("a", "b", "c"), null, false)));

        Assert.assertFalse(new BeanId(new J2EENameImpl("a", "b", "c"), null, false).equals((Object)
                        new BeanId(new J2EENameImpl("a", "b", "c"), 0, false)));
        Assert.assertFalse(new BeanId(new J2EENameImpl("a", "b", "c"), null, false).equals(
                        new BeanId(new J2EENameImpl("a", "b", "c"), 0, false)));

        Assert.assertFalse(new BeanId(new J2EENameImpl("a", "b", "c"), 0, false).equals((Object)
                        new BeanId(new J2EENameImpl("a", "b", "c"), null, false)));
        Assert.assertFalse(new BeanId(new J2EENameImpl("a", "b", "c"), 0, false).equals(
                        new BeanId(new J2EENameImpl("a", "b", "c"), null, false)));

        Assert.assertEquals(new BeanId(new J2EENameImpl("a", "b", "c"), "a", false),
                            new BeanId(new J2EENameImpl("a", "b", "c"), "a", false));
        Assert.assertTrue(new BeanId(new J2EENameImpl("a", "b", "c"), "a", false).equals(
                        new BeanId(new J2EENameImpl("a", "b", "c"), "a", false)));

        Assert.assertEquals(new BeanId(new J2EENameImpl("__homeOfHomes", "__homeOfHomes", "__homeOfHomes"), new J2EENameImpl("a", "b", "c"), true),
                            new BeanId(new J2EENameImpl("__homeOfHomes", "__homeOfHomes", "__homeOfHomes"), new J2EENameImpl("a", "b", "c"), true));
        Assert.assertTrue(new BeanId(new J2EENameImpl("__homeOfHomes", "__homeOfHomes", "__homeOfHomes"), new J2EENameImpl("a", "b", "c"), true).equals(
                        new BeanId(new J2EENameImpl("__homeOfHomes", "__homeOfHomes", "__homeOfHomes"), new J2EENameImpl("a", "b", "c"), true)));
    }

    private static final int TYPE_HOME = 0;
    private static final int TYPE_SLSB = 1;
    private static final int TYPE_SFSB = 2;
    private static final int TYPE_EB = 3;
    private static final int TYPE_MDB = 4;
    private static final int TYPE_SGSB = 5;

    private static final int FLAG_BMT = 0x10;
    private static final int FLAG_VERSIONED = 0x20;

    private static final int PK_SERIALIZABLE = 0;
    private static final int PK_INT = 1;
    private static final int PK_STRING = 3;
    private static final int PK_LONG = 4;
    private static final int PK_BYTES = 8;

    private static BeanMetaData createBMD(String app, String mod, String comp, int type, boolean bmt)
    {
        BeanMetaData bmd = new BeanMetaData(0);
        bmd.container = EJSContainer.getDefaultContainer();
        bmd.enterpriseBeanName = comp;
        bmd.j2eeName = new J2EENameImpl(app, mod, comp);
        EJBApplicationMetaData amd = new EJBApplicationMetaData(null, app, app, false, null, false, false);
        bmd._moduleMetaData = new EJBModuleMetaDataImpl(0, amd);
        bmd._moduleMetaData.ivJ2EEName = new J2EENameImpl(app, mod, null);
        bmd.type = type;
        bmd.usesBeanManagedTx = bmt;
        bmd.homeRecord = new HomeRecord(bmd, bmd.container.getHomeOfHomes());
        return bmd;
    }

    private static BeanMetaData createVersionedBMD(String app, String mod, String comp, int type, boolean bmt)
    {
        BeanMetaData bmd = createBMD(app, mod, comp, type, bmt);
        bmd._moduleMetaData.ivInitData = new ModuleInitData(mod, app);
        bmd._moduleMetaData.setVersionedModuleBaseName(app.substring(0, app.indexOf('-')), mod.substring(0, mod.indexOf('-')));
        return bmd;
    }

    @Test
    public void testHomeBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_HOME, // type
                                   41, 0, 0, 0, // J2EEName length
                                   '_', '_', 'h', 'o', 'm', 'e', 'O', 'f', 'H', 'o', 'm', 'e', 's', '#',
                                   '_', '_', 'h', 'o', 'm', 'e', 'O', 'f', 'H', 'o', 'm', 'e', 's', '#',
                                   '_', '_', 'h', 'o', 'm', 'e', 'O', 'f', 'H', 'o', 'm', 'e', 's',
                                   PK_BYTES, // PK type
                                   'a', '#', 'm', '#', 'c', // PK
        };
        EJSContainer container = new TestEJSContainer();

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertTrue(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertEquals(bid.getJ2EEName(), bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertTrue(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_HOME);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    @Test
    public void testVersionedHomeBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_HOME | FLAG_VERSIONED, // type
                                   41, 0, 0, 0, // J2EEName length
                                   '_', '_', 'h', 'o', 'm', 'e', 'O', 'f', 'H', 'o', 'm', 'e', 's', '#',
                                   '_', '_', 'h', 'o', 'm', 'e', 'O', 'f', 'H', 'o', 'm', 'e', 's', '#',
                                   '_', '_', 'h', 'o', 'm', 'e', 'O', 'f', 'H', 'o', 'm', 'e', 's',
                                   PK_BYTES, // PK type
                                   'a', '#', 'm', '#', 'c', // PK
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createVersionedBMD("a-1.0", "m-1.0", "c", InternalConstants.TYPE_STATELESS_SESSION, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);

        Assert.assertTrue(bid.isHome());
        Assert.assertEquals("a-1.0#m-1.0#c", bid.getJ2EEName().toString());
        Assert.assertEquals(bid.getJ2EEName(), bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertTrue(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_HOME);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    @Test
    public void testStatelessBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_SLSB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_STATELESS_SESSION, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertNull(bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_SLSB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    @Test
    public void testStatelessBMTBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_SLSB | FLAG_BMT, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_STATELESS_SESSION, true));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertNull(bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_SLSB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    @Test
    public void testVersionedStatelessBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_SLSB | FLAG_VERSIONED, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createVersionedBMD("a-1.0", "m-1.0", "c", InternalConstants.TYPE_STATELESS_SESSION, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a-1.0#m-1.0#c", bid.getJ2EEName().toString());
        Assert.assertNull(bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_SLSB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    @Test
    public void testStatefulBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_SFSB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
                                   PK_BYTES, // PK type
                                   1, // PK
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_STATEFUL_SESSION, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertEquals(1, ((TestStatefulSessionKey) bid.getPrimaryKey()).ivValue);

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_SFSB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertEquals(new ByteArray(new byte[] { 1 }), new ByteArray(oaKey.getPrimaryKeyBytes()));
    }

    @Test
    public void testEntityIntegerPKBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_EB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
                                   PK_INT, // PK type
                                   1, 0, 0, 0, // PK
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_BEAN_MANAGED_ENTITY, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertEquals(1, bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_EB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertEquals(new ByteArray(new byte[] { 1, 0, 0, 0 }), new ByteArray(oaKey.getPrimaryKeyBytes()));
    }

    @Test
    public void testEntityLongPKBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_EB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
                                   PK_LONG, // PK type
                                   1, 0, 0, 0, 0, 0, 0, 0, // PK
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_BEAN_MANAGED_ENTITY, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertEquals(1L, bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_EB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertEquals(new ByteArray(new byte[] { 1, 0, 0, 0, 0, 0, 0, 0 }), new ByteArray(oaKey.getPrimaryKeyBytes()));
    }

    @Test
    public void testEntityStringPKBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_EB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
                                   PK_STRING, // PK type
                                   'p', 'k', // PK
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_BEAN_MANAGED_ENTITY, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertEquals("pk", bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_EB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertEquals(new ByteArray(new byte[] { 'p', 'k' }), new ByteArray(oaKey.getPrimaryKeyBytes()));
    }

    @Test
    public void testEntitySerializablePKBytes()
                    throws Exception
    {
        byte[] baseBytes = new byte[] {
                                       (byte) 0xac, (byte) 0xac, // magic
                                       (byte) 0x00, (byte) 0x02, // major version
                                       (byte) 0x00, (byte) 0x01, // minor version
                                       TYPE_EB, // type
                                       5, 0, 0, 0, // J2EEName length
                                       'a', '#', 'm', '#', 'c', // J2EEName
                                       PK_SERIALIZABLE, // PK type
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new ArrayList<Object>());
        oos.close();
        byte[] pkBytes = baos.toByteArray();

        byte[] bytes = new byte[baseBytes.length + pkBytes.length];
        System.arraycopy(baseBytes, 0, bytes, 0, baseBytes.length);
        System.arraycopy(pkBytes, 0, bytes, baseBytes.length, pkBytes.length);

        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_BEAN_MANAGED_ENTITY, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertEquals(new ArrayList<Object>(), bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_EB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertEquals(new ByteArray(pkBytes), new ByteArray(oaKey.getPrimaryKeyBytes()));
    }

    @Test
    public void testMessageDrivenBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_MDB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_MESSAGE_DRIVEN, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertNull(bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_MDB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    @Test
    public void testSingletonBytes()
                    throws Exception
    {
        byte[] bytes = new byte[] {
                                   (byte) 0xac, (byte) 0xac, // magic
                                   (byte) 0x00, (byte) 0x02, // major version
                                   (byte) 0x00, (byte) 0x01, // minor version
                                   TYPE_SGSB, // type
                                   5, 0, 0, 0, // J2EEName length
                                   'a', '#', 'm', '#', 'c', // J2EEName
        };
        EJSContainer container = new TestEJSContainer();
        addHome(createBMD("a", "m", "c", InternalConstants.TYPE_SINGLETON_SESSION, false));

        BeanId bid = BeanId.getBeanId(new ByteArray(bytes), container);
        Assert.assertFalse(bid.isHome());
        Assert.assertEquals("a#m#c", bid.getJ2EEName().toString());
        Assert.assertNull(bid.getPrimaryKey());

        //        EJBOAKeyImpl oaKey = new EJBOAKeyImpl(bytes);
        //        Assert.assertFalse(oaKey.isHome());
        //        Assert.assertEquals(oaKey.getBeanType(), TYPE_SGSB);
        //        Assert.assertEquals("a#m#c", new String(oaKey.getJ2EENameBytes(), "UTF-8"));
        //        Assert.assertNull(oaKey.getPrimaryKeyBytes());
    }

    private static class TestEJSContainer
                    extends EJSContainer
    {
        private final J2EENameFactory j2eeNameFactory = new J2EENameFactoryImpl();

        TestEJSContainer()
        {
            EJSContainer.homeOfHomes = new HomeOfHomes(this, null);
            EJSContainer.defaultContainer = this;
            EJSContainer.sessionKeyFactory = new TestStatefulSessionKeyFactory();
        }

        @Override
        public J2EENameFactory getJ2EENameFactory()
        {
            return j2eeNameFactory;
        }

        @Override
        public HomeOfHomes getHomeOfHomes()
        {
            return EJSContainer.homeOfHomes;
        }

        @Override
        public EJBRuntime getEJBRuntime()
        {
            return (EJBRuntime) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { EJBRuntime.class }, new InvocationHandler()
            {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args)
                                throws Throwable
                {
                    if (method.getName().equals("getBeanOFactory") &&
                        args[0] == BeanOFactoryType.CM_STATELESS_BEANO_FACTORY)
                    {
                        return new CMStatelessBeanOFactory();
                    }

                    if (method.getName().equals("initializeDeferredEJB"))
                    {
                        HomeRecord homeRecord = (HomeRecord) args[0];
                        return new TestEJSHome(homeRecord.bmd);
                    }

                    if (method.getName().equals("createObjectInputStream"))
                    {
                        InputStream input = (InputStream) args[0];
                        return new ObjectInputStream(input);
                    }

                    throw new UnsupportedOperationException(method.toString());
                }
            });
        }
    }

    private static class TestStatefulSessionKeyFactory
                    implements StatefulSessionKeyFactory
    {
        @Override
        public StatefulSessionKey create()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public StatefulSessionKey create(byte[] bytes)
        {
            return new TestStatefulSessionKey(bytes[0]);
        }
    }

    @SuppressWarnings("serial")
    private static class TestStatefulSessionKey
                    implements StatefulSessionKey
    {
        final int ivValue;

        public TestStatefulSessionKey(int value)
        {
            ivValue = value;
        }

        @Override
        public byte[] getBytes()
        {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("serial")
    private static class TestEJSHome
                    extends EJSHome
    {
        TestEJSHome(BeanMetaData bmd)
            throws RemoteException
        {
            beanMetaData = bmd;
            j2eeName = bmd.j2eeName;
            statelessSessionHome = bmd.type == InternalConstants.TYPE_STATELESS_SESSION;
            statefulSessionHome = bmd.type == InternalConstants.TYPE_STATEFUL_SESSION;
            ivSingletonSessionHome = bmd.type == InternalConstants.TYPE_SINGLETON_SESSION;
            messageDrivenHome = bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN;
            enabled = true;
        }
    }
}
