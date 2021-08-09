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
/*
 * Change activity:
 *
 * Reason          Date        Origin       Description
 * --------------- ------      --------     --------------------------------------------
 *                 11 Sep 2006 egglestn     Original
 * ============================================================================
 */
package com.ibm.ws.sib.msgstore.test;

import junit.framework.TestSuite;
import com.ibm.ws.sib.msgstore.MessageStore;

/*
 * author: Sarah Eggleston
 */
public class RestartAfterFailureTest extends MessageStoreTestCase 
{

    public static TestSuite suite() 
    {
        return new TestSuite(RestartAfterFailureTest.class);
    }

    public RestartAfterFailureTest(String str) 
    {
        super(str);
    }

    public void testRestart() 
    {
	MessageStore messageStore = null;
	try
	{
	    // testing the unhealthy start - should work transparently, we should reset the health
	     messageStore = createAndStartPreviouslyUnhealthyMessageStore(true);
        } 
	catch (Exception e) 
	{
            fail(e.toString());
        }

        stopMessageStore(messageStore);
    }

}
