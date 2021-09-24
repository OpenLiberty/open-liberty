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

import java.net.InetAddress;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import io.openliberty.accesslists.AccessListKeysFacade;
/**
 * This is a scaffolding class for Unit Testing the AccessLists logic, it can be
 * used to generate lots of Unit Test data example configurations for access
 * lists and is design to visit the entire state space of equivalence classes of
 * test configs (multiple wildcards is not added yet) if the constructor that
 * consumes a long is used for the range 0..2^ConfigElements
 */
public class UnitTestAccessListConfig implements AccessListKeysFacade {

    // We use some base data that gets selected and chopped about:
    private static final int INVALID = -1;
    private static final String[] STANDARD_ADDRESS_LIST = { "232.45.12.1", "180,89.21.12", "4.9.56.8", "23.9.245.1" };
    private static final String[] STANDARD_HOST_LIST = { "server1.fred.com", "service3.cards.com",
            "myserver.mesith.com" };

    private static final Random rndm = new Random();
    // We cache the configs generated and try to have one instance to represent the
    // invalid configs
    public static final UnitTestAccessListConfig INVALID_CONFIG = new UnitTestAccessListConfig(INVALID);

    // Standard values that can be used in unit tests - set to match the actual host
    // used at runtime
    public static InetAddress TEST_CLIENT = null;
    public static String TEST_CLIENT_HOSTNAME = null;
    public static String TEST_CLIENT_ADDR = null;
    public static String TEST_CLIENT_ADDR_WILD = null;
    public static String TEST_CLIENT_ADDR_IP6 = null;
    public static String TEST_CLIENT_HOSTNAME_WILD = "*.local";
    static {
        try {
            TEST_CLIENT = InetAddress.getLocalHost();
            TEST_CLIENT_HOSTNAME = TEST_CLIENT.getHostName().toLowerCase();
            // Create stable getHostName
            TEST_CLIENT = InetAddress.getByName(TEST_CLIENT_HOSTNAME);
            TEST_CLIENT_ADDR = TEST_CLIENT.getHostAddress();
            TEST_CLIENT_ADDR_IP6 = convertIP4toIP6(TEST_CLIENT_ADDR);

            TEST_CLIENT_ADDR_WILD = "*";
            byte[] addrSplit = TEST_CLIENT.getAddress();
            for (int i = 1; i < addrSplit.length; i++) {
                int a = addrSplit[i] & 0xFF; // fixes -127..128 range
                TEST_CLIENT_ADDR_WILD = TEST_CLIENT_ADDR_WILD + "." + a;
            }

            String[] wildHost = TEST_CLIENT_HOSTNAME.split("\\.");
            TEST_CLIENT_HOSTNAME_WILD = "*";
            for (int i = 1; i < wildHost.length; i++) {
                TEST_CLIENT_HOSTNAME_WILD = TEST_CLIENT_HOSTNAME_WILD + "." + wildHost[i];
            }

        } catch (Throwable e) {
            throw new IllegalArgumentException("Can't use localhost" + e.getMessage());
        }

    }

    /**
     * These are the boolean 'facts' that differentiate different AccessList
     * configurations. The 'client' is the address/host being tested for access
     *
     */
    public enum ConfigElement {
        isClientIP6InAddressIncludeList, isClientIP6InAddressExcludeList, isClientWildcardedInHostIncludeList,
        isClientWildCardedInHostExcludeList, isClientWildcardedInAddressIncludeList,
        isClientWildcardedInAddressExcludeList, isClientInDomainOfHostIncludeList, isClientInDomainOfHostExcludeList,
        isClientInDomainOfAddressIncludeList, isClientInDomainOfAddressExcludeList, isHostIncludeListPresent,
        isHostExcludeListPresent, isAddressIncludeListPresent, isAddressExcludeListPresent;
    }

    // We try not to repeatedly generate the same config
    private static Map<Long, UnitTestAccessListConfig> cache = new HashMap<Long, UnitTestAccessListConfig>();

    // We store the config as a bitset of the Enum above, the value() is the column
    // in the BitSet
    BitSet configState = new BitSet();

    // @formatter:off
    public boolean isClientIP6InAddressIncludeList        (){ return isConfigElementTrue( ConfigElement.isClientIP6InAddressIncludeList        );} 
    public boolean isClientIP6InAddressExcludeList        (){ return isConfigElementTrue( ConfigElement.isClientIP6InAddressExcludeList        );} 
    public boolean isClientWildcardedInHostIncludeList    (){ return isConfigElementTrue( ConfigElement.isClientWildcardedInHostIncludeList    );} 
    public boolean isClientWildCardedInHostExcludeList    (){ return isConfigElementTrue( ConfigElement.isClientWildCardedInHostExcludeList    );} 
    public boolean isClientWildcardedInAddressIncludeList (){ return isConfigElementTrue( ConfigElement.isClientWildcardedInAddressIncludeList );} 
    public boolean isClientWildcardedInAddressExcludeList (){ return isConfigElementTrue( ConfigElement.isClientWildcardedInAddressExcludeList );} 
    public boolean isClientInDomainOfHostIncludeList      (){ return isConfigElementTrue( ConfigElement.isClientInDomainOfHostIncludeList      );} 
    public boolean isClientInDomainOfHostExcludeList      (){ return isConfigElementTrue( ConfigElement.isClientInDomainOfHostExcludeList      );} 
    public boolean isClientInDomainOfAddressIncludeList   (){ return isConfigElementTrue( ConfigElement.isClientInDomainOfAddressIncludeList   );} 
    public boolean isClientInDomainOfAddressExcludeList   (){ return isConfigElementTrue( ConfigElement.isClientInDomainOfAddressExcludeList   );} 
    public boolean isHostIncludeListPresent               (){ return isConfigElementTrue( ConfigElement.isHostIncludeListPresent               );} 
    public boolean isHostExcludeListPresent               (){ return isConfigElementTrue( ConfigElement.isHostExcludeListPresent               );} 
    public boolean isAddressIncludeListPresent            (){ return isConfigElementTrue( ConfigElement.isAddressIncludeListPresent            );} 
    public boolean isAddressExcludeListPresent            (){ return isConfigElementTrue( ConfigElement.isAddressExcludeListPresent            );} 

    public String toString() {
    return this==INVALID_CONFIG?"INVALID_CONFIG":configState.toString() + 
    " isClientIP6InAddressIncludeList       :" + isConfigElementTrue( ConfigElement.isClientIP6InAddressIncludeList        )+", "+ 
    " isClientIP6InAddressExcludeList       :" + isConfigElementTrue( ConfigElement.isClientIP6InAddressExcludeList        )+", "+ 
    " isClientWildcardedInHostIncludeList   :" + isConfigElementTrue( ConfigElement.isClientWildcardedInHostIncludeList    )+", "+ 
    " isClientWildCardedInHostExcludeList   :" + isConfigElementTrue( ConfigElement.isClientWildCardedInHostExcludeList    )+", "+ 
    " isClientWildcardedInAddressIncludeList:" + isConfigElementTrue( ConfigElement.isClientWildcardedInAddressIncludeList )+", "+ 
    " isClientWildcardedInAddressExcludeList:" + isConfigElementTrue( ConfigElement.isClientWildcardedInAddressExcludeList )+", "+ 
    " isClientInDomainOfHostIncludeList     :" + isConfigElementTrue( ConfigElement.isClientInDomainOfHostIncludeList      )+", "+ 
    " isClientInDomainOfHostExcludeList     :" + isConfigElementTrue( ConfigElement.isClientInDomainOfHostExcludeList      )+", "+ 
    " isClientInDomainOfAddressIncludeList  :" + isConfigElementTrue( ConfigElement.isClientInDomainOfAddressIncludeList   )+", "+ 
    " isClientInDomainOfAddressExcludeList  :" + isConfigElementTrue( ConfigElement.isClientInDomainOfAddressExcludeList   )+", "+ 
    " isHostIncludeListPresent              :" + isConfigElementTrue( ConfigElement.isHostIncludeListPresent               )+", "+ 
    " isHostExcludeListPresent              :" + isConfigElementTrue( ConfigElement.isHostExcludeListPresent               )+", "+ 
    " isAddressIncludeListPresent           :" + isConfigElementTrue( ConfigElement.isAddressIncludeListPresent            )+", "+ 
    " isAddressExcludeListPresent           :" + isConfigElementTrue( ConfigElement.isAddressExcludeListPresent            )+" "; 
    }
    // @formatter:on

    /**
     * Is this factor true for the current configuration
     * 
     * @param configElement
     * @return true if the factor (Enum.value() column in BitSet) is true
     */
    private boolean isConfigElementTrue(ConfigElement configElement) {
        if (this == INVALID_CONFIG) {
            return false;
        }
        return isFlagSet(configElement, configState);
    }

    private boolean isFlagSet(Enum<?> column, BitSet bitset) {
        return bitset.get(column.ordinal());
    }

    private UnitTestAccessListConfig(BitSet c) {
        configState = c;
    }

    /**
     * Create a scaffold config from a long, the 'bits' in the long represent
     * ConfigElements being true.
     * 
     * @param config - the equivalence class index of the config
     */
    public UnitTestAccessListConfig(long config) {
        this(BitSet.valueOf(new long[] { config }));
        if (config < -1 || config >= Math.pow(2, ConfigElement.values().length)) {
            throw new InvalidParameterException("0..255 or -1(invalid) allowed not:" + config);
        }
    }

    @Override
    public String[] getAddressExcludeList() {
        String[] result = null;
        if (isAddressExcludeListPresent()) {
            result = Arrays.copyOf(STANDARD_ADDRESS_LIST, STANDARD_ADDRESS_LIST.length);
            if (isClientInDomainOfAddressExcludeList()) {
                int i = rndm.nextInt(STANDARD_ADDRESS_LIST.length);
                if (isClientWildcardedInAddressExcludeList()) {
                    result[i] = TEST_CLIENT_ADDR_WILD;
                } else if (isClientIP6InAddressExcludeList()) {
                    result[i] = TEST_CLIENT_ADDR_IP6;
                } else {
                    result[i] = TEST_CLIENT_ADDR;
                }
            }
        }
        return result;
    }

    @Override
    public String[] getAddressIncludeList() {
        String[] result = null;
        if (isAddressIncludeListPresent()) {
            result = Arrays.copyOf(STANDARD_ADDRESS_LIST, STANDARD_ADDRESS_LIST.length);
            if (isClientInDomainOfAddressIncludeList()) {
                int i = rndm.nextInt(STANDARD_ADDRESS_LIST.length);
                if (isClientWildcardedInAddressIncludeList()) {
                    result[i] = TEST_CLIENT_ADDR_WILD;
                } else if (isClientIP6InAddressIncludeList()) {
                    result[i] = TEST_CLIENT_ADDR_IP6;
                } else {
                    result[i] = TEST_CLIENT_ADDR;
                }
            }
        }
        return result;
    }

    @Override
    public String[] getHostNameExcludeList() {
        String[] result = null;
        if (isHostExcludeListPresent()) {
            result = Arrays.copyOf(STANDARD_HOST_LIST, STANDARD_HOST_LIST.length);
            if (isClientInDomainOfHostExcludeList()) {
                int i = rndm.nextInt(STANDARD_HOST_LIST.length);
                if (isClientWildCardedInHostExcludeList()) {
                    result[i] = TEST_CLIENT_HOSTNAME_WILD;
                } else {
                    result[i] = TEST_CLIENT_HOSTNAME;
                }
            }
        }
        return result;
    }

    @Override
    public String[] getHostNameIncludeList() {
        String[] result = null;
        if (isHostIncludeListPresent()) {
            result = Arrays.copyOf(STANDARD_HOST_LIST, STANDARD_HOST_LIST.length);
            if (isClientInDomainOfHostIncludeList()) {
                int i = rndm.nextInt(STANDARD_HOST_LIST.length);
                if (isClientWildcardedInHostIncludeList()) {
                    result[i] = TEST_CLIENT_HOSTNAME_WILD;
                } else {
                    result[i] = TEST_CLIENT_HOSTNAME;
                }
            }
        }
        return result;
    }

    @Override
    public boolean getCaseInsensitiveHostnames() {
        return true;
    }

    /**
     * Is this config class a valid one in the real world?
     * We allow construction of different invalid configs but only store one
     * instance in the cache for all of them
     * 
     * @return true if this config could occur in real life
     */
    private boolean valid() {
        
        // We use a standard 
        if (this == INVALID_CONFIG) {
            return false;
        }

        // Check all the logical consistencies
        long[] array = configState.toLongArray();
        
        // All out bits fit in the first Long of the array
        if (array.length > 0) {
            if (array[0] == -1L) {
                return false; // We should not see this as currently used rather INVALID_CONFIG
            }
        }

        // Do the checks:
        if (!isAddressExcludeListPresent() && isClientInDomainOfAddressExcludeList())
            return false;

        else if (!isAddressIncludeListPresent() && isClientInDomainOfAddressIncludeList())
            return false;

        else if (!isHostExcludeListPresent() && isClientInDomainOfHostExcludeList())
            return false;

        else if (!isHostIncludeListPresent() && isClientInDomainOfHostIncludeList())
            return false;

        else if ((!isAddressExcludeListPresent() || !isClientInDomainOfAddressExcludeList())
                && (isClientIP6InAddressExcludeList() || isClientWildcardedInAddressExcludeList()))
            return false;

        else if ((!isAddressIncludeListPresent() || !isClientInDomainOfAddressIncludeList())
                && (isClientIP6InAddressIncludeList() || isClientWildcardedInAddressIncludeList()))
            return false;

        else if ((!isHostExcludeListPresent() || !isClientInDomainOfHostExcludeList())
                && isClientWildCardedInHostExcludeList())
            return false;

        else if ((!isHostIncludeListPresent() || !isClientInDomainOfHostIncludeList())
                && isClientWildcardedInHostIncludeList())
            return false;

        // Nothing wrong
        return true;
    }

    /**
     * Get from the cache or make a Config for this state index
     * 
     * @param i - the long that has bits that represent the ConfigElements
     * @return the config for the state index or the standard INVALID instance
     */
    static UnitTestAccessListConfig get(long i) {
        if (!cache.containsKey(i)) {
            UnitTestAccessListConfig c = new UnitTestAccessListConfig(i);
            if (c.valid()) {
                cache.put(i, c);
            } else {
                cache.put(i, INVALID_CONFIG);
            }
        }
        return cache.get(i);
    }

    /**
     * Would this state index produce a valid Config?
     * 
     * @param i
     * @return true if the resulting Config would be possible
     */
    public static boolean validConfig(long i) {
        UnitTestAccessListConfig c = get(i);
        return c != UnitTestAccessListConfig.INVALID_CONFIG;
    }

    /**
     * Provide an opinion on whether this config would allow access to the
     * assumed Client
     * 
     * @return true if access should be denied
     */
    public boolean accessDenied() {

        boolean denied = false;

        boolean includesExist = isAddressIncludeListPresent() || isHostIncludeListPresent();
        boolean included = isClientInDomainOfAddressIncludeList() || isClientInDomainOfHostIncludeList();
        boolean excluded = isClientInDomainOfAddressExcludeList() || isClientInDomainOfHostExcludeList();

        if (includesExist && !included) {
            denied = true;
        }

        if (excluded) {
            denied = true;
        }

        return denied;
    }

    /**
     * This method is taken directly from tWas test code, it is used to
     * help generate AccessLists that have ip6 addresses for the client
     * that are in ip6 format.
     * 
     * @param ip4
     * @return
     */
    public static String convertIP4toIP6(String ip4) {
        String ip6 = null;

        // if already in ipv6, then return it
        if (ip4.indexOf(":") != -1) {
            return ip4;
        }

        int startIndex = 0;
        int endIndex = ip4.indexOf(".");
        String ip41 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        startIndex = endIndex + 1;
        endIndex = ip4.indexOf(".", endIndex + 1);
        String ip42 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        startIndex = endIndex + 1;
        endIndex = ip4.indexOf(".", endIndex + 1);
        String ip43 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        startIndex = endIndex + 1;
        endIndex = ip4.length();
        String ip44 = Integer.toHexString(Integer.parseInt(ip4.substring(startIndex, endIndex)));

        ip6 = "0:0:0:0:" + ip41 + ":" + ip42 + ":" + ip43 + ":" + ip44;

        return ip6;
    }

}