/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.accesslists.internal.test;

import org.junit.Assert;
import org.junit.Test;

import io.openliberty.accesslists.AccessListKeysFacade;
import io.openliberty.accesslists.AddressAndHostNameAccessLists;
import io.openliberty.accesslists.filterlist.FilterList;
import io.openliberty.accesslists.filterlist.FilterListFastStr;
import io.openliberty.accesslists.filterlist.FilterListStr;
import io.openliberty.accesslists.internal.test.UnitTestAccessListConfig.ConfigElement;

/**
 * Unit test the tcpOptions denied function
 */
public class AccessListsUnitTests {

    @Test
    public void testDeniedLogic() {

        /*
         * We enumerate factors that differentiate AccessList configuration in
         * {@link ConfigElement} then we can treat the whole state space of 
         * equivalence case for unit test as a binary number, and increment through
         * that number exploring all the different 'types' of AccessList configuration
         * if a '1' digit in the binary representation of the number represents that
         * factor being present and a '0' meaning the opposite. 
         */
        
        /* There are 2^factors of equivalence classes for unit testing test data 
         * although quite a few of these will be invalid or impossible. 
         */
        int equivalentConfigs = (int) Math.pow(2, ConfigElement.values().length);
        
        for (long i = 0; i < equivalentConfigs; i++) {
            if (UnitTestAccessListConfig.validConfig(i)) {
                UnitTestAccessListConfig config = UnitTestAccessListConfig.get(i);
                AddressAndHostNameAccessLists impl = AddressAndHostNameAccessLists.getInstance(config);
                if (impl == null) {
                    Assert.assertTrue("Only empty access lists (or INVALID) have null TCP AccesList instances for" + i,
                            i == 0L || i == -1L);
                } else {
                    Assert.assertEquals("accessDenied not equal for:" + config + "\nscaffold=" +  config.accessDenied() + "\nimpl=" + impl.accessDenied(UnitTestAccessListConfig.TEST_CLIENT) + " \nfor " + UnitTestAccessListConfig.TEST_CLIENT_HOSTNAME + " "+ UnitTestAccessListConfig.TEST_CLIENT_ADDR ,
                            config.accessDenied(), impl.accessDenied(UnitTestAccessListConfig.TEST_CLIENT) );
                }
            }
        }
    }
}
