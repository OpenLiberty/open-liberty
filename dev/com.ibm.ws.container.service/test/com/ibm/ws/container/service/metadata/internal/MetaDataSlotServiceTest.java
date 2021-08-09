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
package com.ibm.ws.container.service.metadata.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

public class MetaDataSlotServiceTest {
    private final Mockery mockery = new Mockery();
    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private final MetaDataServiceImpl metaDataService = new MetaDataServiceImpl();
    private MetaDataSlotServiceImpl service;

    @Before
    public void setUp() {
        mockery.checking(new Expectations() {
            {
                allowing(componentContext).getUsingBundle();
            }
        });

        service = newService();
    }

    private MetaDataSlotServiceImpl newService() {
        MetaDataSlotServiceImpl result = new MetaDataSlotServiceImpl();
        result.setMetaDataService(metaDataService);
        result.activate(componentContext);
        return result;
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.deactivate();
        }
    }

    private void deactivate() {
        service.deactivate();
        service = null;
    }

    @Test
    public void testReserve() {
        service.reserveMetaDataSlot(ApplicationMetaData.class);
        service.reserveMetaDataSlot(ModuleMetaData.class);
        service.reserveMetaDataSlot(ComponentMetaData.class);
        service.reserveMetaDataSlot(MethodMetaData.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidReserve() {
        service.reserveMetaDataSlot(TestApplicationMetaData.class);
    }

    /**
     * Verify that data can be set in and retrieved from a slot.
     */
    @Test
    public void testSlot() {
        ApplicationMetaData amd = new TestApplicationMetaData();
        MetaDataSlot slot = service.reserveMetaDataSlot(ApplicationMetaData.class);
        amd.setMetaData(slot, "abc");
        Assert.assertEquals("abc", amd.getMetaData(slot));
    }

    /**
     * Verify that many slots can be reserved, that each slot is distinct, and
     * that the slots can be set in any order.
     */
    @Test
    public void testSlots() {
        for (int round = 0; round < 2; round++) {
            int[] indices = new int[100];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = round == 0 ? i : 100 - i - 1;
            }

            ApplicationMetaData amd = new TestApplicationMetaData();
            MetaDataSlot[] slots = new MetaDataSlot[100];
            Object[] data = new Object[slots.length];

            for (int i = 0; i < indices.length; i++) {
                slots[i] = service.reserveMetaDataSlot(ApplicationMetaData.class);
                data[i] = "abc" + i;
            }

            for (int i : indices) {
                amd.setMetaData(slots[i], data[i]);
            }

            for (int i : indices) {
                Assert.assertSame(data[i], amd.getMetaData(slots[i]));
            }
        }
    }

    /**
     * Verify that {@link MetaDataImpl#getMetaData} returns null if unset.
     */
    @Test
    public void testEmptySlot() throws Exception {
        ApplicationMetaData amd = new TestApplicationMetaData();
        Container container = mockery.mock(Container.class);
        metaDataService.fireApplicationMetaDataCreated(amd, container);

        for (int i = 0; i < 100; i++) {
            MetaDataSlot slot = service.reserveMetaDataSlot(ApplicationMetaData.class);
            Assert.assertNull(amd.getMetaData(slot));
            amd.setMetaData(slot, true);
        }

        service.deactivate();
        service = newService();

        for (int i = 0; i < 100; i++) {
            MetaDataSlot slot = service.reserveMetaDataSlot(ApplicationMetaData.class);
            Assert.assertNull(amd.getMetaData(slot));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testDeactivatedReserve() {
        MetaDataSlotService service = this.service;
        deactivate();
        service.reserveMetaDataSlot(ApplicationMetaData.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testDeactivatedGetMetaData() {
        MetaDataSlot slot = service.reserveMetaDataSlot(ApplicationMetaData.class);
        deactivate();
        new TestApplicationMetaData().getMetaData(slot);
    }

    @Test(expected = IllegalStateException.class)
    public void testDeactivatedSetMetaData() {
        MetaDataSlot slot = service.reserveMetaDataSlot(ApplicationMetaData.class);
        deactivate();
        new TestApplicationMetaData().setMetaData(slot, null);
    }

    /**
     * Verify that the data for a slot is cleared when its owning service is
     * deactivated.
     */
    @Test
    public void testSlotGC() {
        MetaDataServiceImpl metaDataService = new MetaDataServiceImpl();
        ApplicationMetaData metaData = new TestApplicationMetaData();
        for (int i = 0; i < 256; i++) {
            MetaDataSlotServiceImpl service = new MetaDataSlotServiceImpl();
            service.setMetaDataService(metaDataService);
            service.activate(componentContext);

            MetaDataSlot slot = service.reserveMetaDataSlot(ApplicationMetaData.class);
            metaData.setMetaData(slot, new Object[] { slot, new byte[16 * 1024 * 1024] });

            service.deactivate();
        }
    }
}
