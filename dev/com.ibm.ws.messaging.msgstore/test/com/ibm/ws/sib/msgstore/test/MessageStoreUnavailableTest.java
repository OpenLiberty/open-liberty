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
package com.ibm.ws.sib.msgstore.test;

/*
 * Change activity:
 *
 *  Reason         Date    Origin    Description
 * ------------- -------- --------- -------------------------------------------
 * 326323        02/12/05  gareth   Throw checked exception when MS is stopped
 * 341158        13/03/06  gareth   Make better use of LoggingTestCase
 * 91633         21/01/13  sharath  Liberty: Changes indtroduced due to fixing defect 91633
 * ============================================================================
 */

import junit.framework.TestSuite;

import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreUnavailableException;

public class MessageStoreUnavailableTest extends MessageStoreTestCase
{
    public MessageStoreUnavailableTest(String name)
    {
        super(name);

        //turnOnTrace();
    }

    public static TestSuite suite(String persistence)
    {
        TestSuite suite = new TestSuite();

        MessageStoreUnavailableTest test = new MessageStoreUnavailableTest("testMessageStoreUnavailable");
        test.setPersistence(persistence);
        suite.addTest(test);

        test = new MessageStoreUnavailableTest("testMessageStoreFailedToStart");
        test.setPersistence(persistence);
        suite.addTest(test);

        return suite;
    }

    public void testMessageStoreUnavailable()
    {
        print("************ testMessageStoreUnavailable *************");
        print("*                                                    *");

        if (PERSISTENCE != null)
        {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MessageStore MS = null;

        // Start the MS to give us a handle to use, this should SUCCEED
        try
        {
            MS = createAndStartMessageStore(true, PERSISTENCE);
            long id = MS.getUniqueLockID(0);

            print("* Create and start MessageStore            - SUCCESS *");
        } catch (Exception e)
        {
            print("* Create and start MessageStore            - FAILED  *");
            fail("Exception thrown during MessageStore create!");
        }

        if (MS != null)
        {
            stopMessageStore(MS);

            print("* Stop MessageStore                        - SUCCESS *");
        }

        // The following attempts to access the MS interface should all
        // fail with a MessageStoreUnavailableException.
        try
        {
            MS.add(null, null);

            print("* Fail to call add()                       - FAILED  *");
            fail("findFirstMatching() successful after MessageStore was stopped!");
        } catch (MessageStoreUnavailableException msue)
        {
            print("* Fail to call add()                       - SUCCESS *");
        } catch (Exception e)
        {
            print("* Fail to call add()                       - FAILED  *");
            fail("add() threw unexpected exception after MessageStore was stopped!");
        }

        try
        {
            ItemStream is = MS.findFirstMatching(null);

            print("* Fail to call findFirstMatching()         - FAILED  *");
            fail("findFirstMatching() successful after MessageStore was stopped!");
        } catch (MessageStoreUnavailableException msue)
        {
            print("* Fail to call findFirstMatching()         - SUCCESS *");
        } catch (Exception e)
        {
            print("* Fail to call findFirstMatching()         - FAILED  *");
            fail("findFirstMatching() threw unexpected exception after MessageStore was stopped!");
        }

        try
        {
            ItemStream is = MS.removeFirstMatching(null, null);

            print("* Fail to call removeFirstMatching()       - FAILED  *");
            fail("removeFirstMatching() successful after MessageStore was stopped!");
        } catch (MessageStoreUnavailableException msue)
        {
            print("* Fail to call removeFirstMatching()       - SUCCESS *");
        } catch (Exception e)
        {
            print("* Fail to call removeFirstMatching()       - FAILED  *");
            fail("removeFirstMatching() threw unexpected exception after MessageStore was stopped!");
        }

        try
        {
            long id = MS.getUniqueLockID(0);

            print("* Fail to call getUniqueLockID()           - FAILED  *");
            fail("getUniqueLockID() successful after MessageStore was stopped!");
        } catch (MessageStoreUnavailableException msue)
        {
            print("* Fail to call getUniqueLockID()           - SUCCESS *");
        } catch (Exception e)
        {
            print("* Fail to call getUniqueLockID()           - FAILED  *");
            fail("getUniqueLockID() threw unexpected exception after MessageStore was stopped!");
        }

        try
        {
            long id = MS.getUniqueTickCount();

            print("* Fail to call getUniqueTickCount()        - FAILED  *");
            fail("getUniqueTickCount() successful after MessageStore was stopped!");
        } catch (MessageStoreUnavailableException msue)
        {
            print("* Fail to call getUniqueTickCount()        - SUCCESS *");
        } catch (Exception e)
        {
            print("* Fail to call getUniqueTickCount()        - FAILED  *");
            fail("getUniqueTickCount() threw unexpected exception after MessageStore was stopped!");
        }

        print("*                                                    *");
        print("************ testMessageStoreUnavailable *************");
    }

    public void testMessageStoreFailedToStart()
    {
        print("*********** testMessageStoreFailedToStart ************");
        print("*                                                    *");

        if (PERSISTENCE != null)
        {
            print("* PersistenceManager Used:                           *");

            int length = PERSISTENCE.length();

            print("* - " + PERSISTENCE.substring(length - 48) + " *");
        }

        MessageStore MS = null;

        // Start the MS with incorrect persistence class, this should FAIL
        try
        {
            MS = createMessageStore(true, PERSISTENCE + "BLAH!");

            try {
                MS.start();
            } catch (Exception e) {
                // No FFDC Needed
            }

            print("* Create MessageStore and kill in start()  - SUCCESS *");
        } catch (Exception e)
        {
            print("* Create MessageStore and kill in start()  - FAILED  *");
            fail("Exception thrown during MessageStore create!");
        }

        // The following attempts to access the MS interface should now all
        // fail with a MessageStoreUnavailableException that has a chained 
        // exception that was thrown at startup.
        try
        {
            MS.add(null, null);

            print("* Fail to call add()                       - FAILED  *");
            fail("findFirstMatching() successful after MessageStore failed to start!");
        } catch (MessageStoreUnavailableException msue)
        {
            if (msue.getCause() != null)
            {
                print("* Fail to call add()                       - SUCCESS *");
            }
            else
            {
                print("* Fail to call add()                       - FAILED  *");
                fail("No chained exception was included in MessageStoreUnavailableException!");
            }
        } catch (Exception e)
        {
            print("* Fail to call add()                       - FAILED  *");
            fail("add() threw unexpected exception after MessageStore failed to start!");
        }

        try
        {
            ItemStream is = MS.findFirstMatching(null);

            print("* Fail to call findFirstMatching()         - FAILED  *");
            fail("findFirstMatching() successful after MessageStore failed to start!");
        } catch (MessageStoreUnavailableException msue)
        {
            if (msue.getCause() != null)
            {
                print("* Fail to call findFirstMatching()         - SUCCESS *");
            }
            else
            {
                print("* Fail to call findFirstMatching()         - FAILED  *");
                fail("No chained exception was included in MessageStoreUnavailableException!");
            }
        } catch (Exception e)
        {
            print("* Fail to call findFirstMatching()         - FAILED  *");
            fail("findFirstMatching threw unexpected exception after MessageStore failed to start!");
        }

        try
        {
            ItemStream is = MS.removeFirstMatching(null, null);

            print("* Fail to call removeFirstMatching()       - FAILED  *");
            fail("removeFirstMatching() successful after MessageStore failed to start!");
        } catch (MessageStoreUnavailableException msue)
        {
            if (msue.getCause() != null)
            {
                print("* Fail to call removeFirstMatching()       - SUCCESS *");
            }
            else
            {
                print("* Fail to call removeFirstMatching()       - FAILED  *");
                fail("No chained exception was included in MessageStoreUnavailableException!");
            }
        } catch (Exception e)
        {
            print("* Fail to call removeFirstMatching()       - FAILED  *");
            fail("removeFirstMatching() threw unexpected exception after MessageStore failed to start!");
        }

        try
        {
            long id = MS.getUniqueLockID(0);

            print("* Fail to call getUniqueLockID()           - FAILED  *");
            fail("getUniqueLockID() successful after MessageStore failed to start!");
        } catch (MessageStoreUnavailableException msue)
        {
            if (msue.getCause() != null)
            {
                print("* Fail to call getUniqueLockID()           - SUCCESS *");
            }
            else
            {
                print("* Fail to call getUniqueLockID()           - FAILED  *");
                fail("No chained exception was included in MessageStoreUnavailableException!");
            }
        } catch (Exception e)
        {
            print("* Fail to call getUniqueLockID()           - FAILED  *");
            fail("getUniqueLockID() threw unexpected exception after MessageStore failed to start!");
        }

        try
        {
            long id = MS.getUniqueTickCount();

            print("* Fail to call getUniqueTickCount()        - FAILED  *");
            fail("getUniqueTickCount() successful after MessageStore failed to start!");
        } catch (MessageStoreUnavailableException msue)
        {
            if (msue.getCause() != null)
            {
                print("* Fail to call getUniqueTickCount()        - SUCCESS *");
            }
            else
            {
                print("* Fail to call getUniqueTickCount()        - FAILED  *");
                fail("No chained exception was included in MessageStoreUnavailableException!");
            }
        } catch (Exception e)
        {
            print("* Fail to call getUniqueTickCount()        - FAILED  *");
            fail("getUniqueTickCount() threw unexpected exception after MessageStore failed to start!");
        }

        print("*                                                    *");
        print("*********** testMessageStoreFailedToStart ************");
    }
}
