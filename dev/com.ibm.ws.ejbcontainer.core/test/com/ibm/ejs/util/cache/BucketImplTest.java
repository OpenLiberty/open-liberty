/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

import org.junit.Assert;
import org.junit.Test;

public class BucketImplTest
{
    private static final int DEFAULT_CAPACITY = BucketImpl.DEFAULT_CAPACITY;

    @Test
    public void testSize()
    {
        BucketImpl bucket = new BucketImpl();
        Assert.assertTrue(bucket.isEmpty());
        Assert.assertEquals(0, bucket.size());

        for (int i = 0; i < DEFAULT_CAPACITY + 1; i++)
        {
            bucket.insertByKey(i, i);
            Assert.assertFalse(bucket.isEmpty());
            Assert.assertEquals(i + 1, bucket.size());
        }

        for (int i = DEFAULT_CAPACITY; i > 0; i--)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
            Assert.assertFalse(bucket.isEmpty());
            Assert.assertEquals(i, bucket.size());
        }

        Assert.assertEquals(0, bucket.removeByKey(0).getObject());
        Assert.assertTrue(bucket.isEmpty());
        Assert.assertEquals(0, bucket.size());
    }

    @Test
    public void testToArray()
    {
        BucketImpl bucket = new BucketImpl();
        Element[] array;
        Element dummyElement = new Element(true);

        array = new Element[0];
        bucket.toArray(array);

        array = new Element[] { dummyElement };
        bucket.toArray(array);
        Assert.assertSame(dummyElement, array[0]);

        bucket.insertByKey(0, 0);
        bucket.toArray(array);
        Assert.assertEquals(0, array[0].getObject());
        Assert.assertEquals(0, bucket.findByKey(0).getObject());

        array = new Element[] { null, dummyElement };
        bucket.toArray(array);
        Assert.assertEquals(0, array[0].getObject());
        Assert.assertSame(dummyElement, array[1]);

        bucket.insertByKey(1, 1);
        bucket.toArray(array);
        Assert.assertEquals(0, array[0].getObject());
        Assert.assertEquals(1, array[1].getObject());

        Assert.assertEquals(0, bucket.removeByKey(0).getObject());
        array[1] = dummyElement;
        bucket.toArray(array);
        Assert.assertEquals(1, array[0].getObject());
    }

    @Test
    public void testAddMany()
    {
        BucketImpl bucket = new BucketImpl();
        int num = DEFAULT_CAPACITY * 10;

        for (int i = 0; i < num; i++)
        {
            bucket.insertByKey(i, i);
            Assert.assertEquals(i + 1, bucket.size());
        }

        for (int i = 0; i < num; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testAddCopy()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove more than half from head.
        for (int i = 0; i < DEFAULT_CAPACITY / 2 + 1; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        // Insert, force copy.
        bucket.insertByKey(DEFAULT_CAPACITY, DEFAULT_CAPACITY);

        Assert.assertEquals(DEFAULT_CAPACITY / 2, bucket.size());

        // Ensure remaining elements are valid.
        for (int i = DEFAULT_CAPACITY / 2 + 1; i < DEFAULT_CAPACITY + 1; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testAddRealloc()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove less than half from head.
        for (int i = 0; i < DEFAULT_CAPACITY / 2 - 1; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        // Insert, force realloc since array too small for copy.
        bucket.insertByKey(DEFAULT_CAPACITY, DEFAULT_CAPACITY);

        Assert.assertEquals(DEFAULT_CAPACITY / 2 + 2, bucket.size());

        // Ensure remaining elements are valid.
        for (int i = DEFAULT_CAPACITY / 2 - 1; i < DEFAULT_CAPACITY + 1; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testRemoveHead()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove less than half from head.
        for (int i = 0; i < DEFAULT_CAPACITY / 2; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        Assert.assertEquals(DEFAULT_CAPACITY / 2, bucket.size());

        // Ensure remaining elements are valid.
        for (int i = DEFAULT_CAPACITY / 2; i < DEFAULT_CAPACITY; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testRemoveHeadFromMiddle()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove half from head.
        for (int i = DEFAULT_CAPACITY / 2 - 1; i >= 0; i--)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        Assert.assertEquals(DEFAULT_CAPACITY / 2, bucket.size());

        // Ensure remaining elements are valid.
        for (int i = DEFAULT_CAPACITY / 2; i < DEFAULT_CAPACITY; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testRemoveTail()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove half from tail.
        for (int i = DEFAULT_CAPACITY - 1; i >= DEFAULT_CAPACITY / 2; i--)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        Assert.assertEquals(DEFAULT_CAPACITY / 2, bucket.size());

        // Ensure remaining elements are valid.
        for (int i = 0; i < DEFAULT_CAPACITY / 2; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testRemoveTailFromMiddle()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove half from tail.
        for (int i = DEFAULT_CAPACITY / 2; i < DEFAULT_CAPACITY; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        Assert.assertEquals(DEFAULT_CAPACITY / 2, bucket.size());

        // Ensure remaining elements are valid.
        for (int i = 0; i < DEFAULT_CAPACITY / 2; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }

    @Test
    public void testRemoveAllFromHead()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove all from head.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        Assert.assertEquals(0, bucket.size());
    }

    @Test
    public void testRemoveAllFromTail()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill to capacity.
        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
        }

        // Remove all from tail.
        for (int i = DEFAULT_CAPACITY - 1; i >= 0; i--)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
        }

        Assert.assertEquals(0, bucket.size());
    }

    @Test
    public void testRemoveAndAdd()
    {
        BucketImpl bucket = new BucketImpl();

        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            bucket.insertByKey(i, i);
            Assert.assertEquals(i + 1, bucket.size());
        }

        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
            Assert.assertEquals(DEFAULT_CAPACITY - 1, bucket.size());
            int value = i + DEFAULT_CAPACITY;
            bucket.insertByKey(value, value);
            Assert.assertEquals(DEFAULT_CAPACITY, bucket.size());
        }

        for (int i = 0; i < DEFAULT_CAPACITY; i++)
        {
            int value = i + DEFAULT_CAPACITY;
            Assert.assertEquals(value, bucket.findByKey(value).getObject());
        }
    }

    @Test
    public void testGrowAndRemoveAndGrow()
    {
        BucketImpl bucket = new BucketImpl();

        // Fill the array, grow it (1.5x), and fill it again.
        int maxEarly = DEFAULT_CAPACITY + (DEFAULT_CAPACITY / 2);
        for (int i = 0; i < maxEarly; i++)
        {
            bucket.insertByKey(i, i);
            Assert.assertEquals(i + 1, bucket.size());
        }

        // Remove half (rounded down) of the elements.
        int numRemove = ((DEFAULT_CAPACITY + (DEFAULT_CAPACITY / 2)) / 2) + 1;
        for (int i = 0; i < numRemove; i++)
        {
            Assert.assertEquals(i, bucket.removeByKey(i).getObject());
            Assert.assertEquals(maxEarly - i - 1, bucket.size());
        }

        // Grow the array (2.25x capacity).
        int maxLate = DEFAULT_CAPACITY * 2;
        for (int i = maxEarly; i < maxLate; i++)
        {
            bucket.insertByKey(i, i);
            Assert.assertEquals(i - numRemove + 1, bucket.size());
        }

        // Ensure that growing the array with half of its elements missing does
        // not improperly copy elements.
        for (int i = numRemove; i < maxLate; i++)
        {
            Assert.assertEquals(i, bucket.findByKey(i).getObject());
        }
    }
}
