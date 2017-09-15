/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/* ************************************************************************** */
/**
 * A Universally unique identifier for use at runtime.
 * 
 * The UUID is an immutable object, it is NOT serializable (but can be converted
 * to and constructed from a byte array), but is clonable (but the clone method
 * just returns "this")
 * 
 * The UUIDs are comparable, but this just establishes an ordering amongst them,
 * the ordering has no semantic meaning (e.g. they are NOT time ordered).
 * 
 * @author David Vines
 * 
 */
/* ************************************************************************** */
public class UUID implements Cloneable, Comparable
{
    private static final TraceComponent tc = Tr.register(UUID.class, null, null);

    private static final String SCCSID = "@(#) 1.9 ws/code/utils/src/com/ibm/ws/util/UUID.java, WAS.utils, ASV 3/5/04 11:40:54 [9/7/12 10:20:04]";

    private static final Object _LOCK = new Object(); // For synchronization

    private static final long _CLASS_LOAD_SYSTEM_TIME = System.currentTimeMillis();
    private static final long _CLASS_LOAD_NANO_TIME = System.nanoTime();
    private static final long NANOS_IN_A_MILLISECOND = 1000000L;

    private static long _lastTime = 0; // The last time we generated a unique key
    private static int _clockSeq = 0;

    private static final int _PID = getPID();

    private static final long _IP_ADDRESS = getIPAddress();

    private static final long _OLD_VERSION_ID = 0x2000000000000000L;
    private static final long _CURRENT_VERSION_ID = 0x4000000000000000L;
    private static final long _VERSION_MASK = 0xF000000000000000L;

    private static final char _NIBBLE[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private long _uuidHigh, _uuidLow; // The current unique key

    private transient String _cachedString = null;
    private transient byte[] _cachedBytes = null;

    private boolean _unexpectedFormat = false;
    private boolean _hashed = false;
    private int _hashCode = 0;

    /* ------------------------------------------------------------------------ */
    /* UUID method */
    /* ------------------------------------------------------------------------ */
    /**
     * Generate a new uuid
     * 
     * @author David Vines
     * 
     */
    /* ------------------------------------------------------------------------ */
    public UUID()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "UUID");

        /**************************************************************************
         * UUID Format: LLLLLLLL-MMMM-HHHH-SSSS-PPPPIIIIIIII
         * 
         * LLLLLLLL Low 32 bits of timestamp, represented in big-endian order
         * MMMM mid 16 bits of timestamp, represented in big-endian order
         * HHHH high 16 bits of timestamp, divided into
         * <4 bit version><high 12 bit timestamp>, in big-endian order.
         * Default <4 bit version> = 0001
         * SSSS clock sequence, divided into
         * <3 bit reserved field><high 5 bit sequence><low 8 bit sequence>
         * Default <3 bit reserved field> = 111
         * PPPP 16 bit process id
         * IIIIIIII 32 bit IP address
         *************************************************************************/

        int ourSeq;

        long time;

        // set last timestamp and current clock sequence
        // Need mutex semaphor to protect last timestamp and clockSeq
        synchronized (_LOCK)
        {
            /* Get time since 00:00:00 GMT, 01/01/70 (64-bit timestamp). */
            time = getTime();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "time = " + time);

            // Check to see if this UUID is being generated in the same time
            // slice as the last one.
            if (_lastTime == time)
            {
                // The generation is being doing in the same time slice so
                // use clockSeq to distinguish between the two UUIDs.

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Times match, incrementing and using clockSeq");

                _clockSeq++;

                // The ourSeq portion of the UUID is only 13 bits in length
                // 2^13 = 8192 so we must wrap clockSeq to zero once it reaches
                // 8192 and ensure that we (and all other threads waiting for
                // the _LOCK) wait for long enough to make the time change. 
                if (_clockSeq == 8192)
                {
                    _clockSeq = 0;

                    time = getTime();

                    while (_lastTime == time) // If you're lucky the time's already moved on!
                    {
                        try
                        {
                            Thread.sleep(1);
                        } catch (InterruptedException e)
                        {
                            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.util.UUID.<init>", "166");

                            // We just want the time to change, so no action is required.
                        }

                        time = getTime();
                    }

                    // We've moved into a new time slice so update _lastTime
                    _lastTime = time;
                }
            }
            else
            {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Times do no match");

                // We reset clockSeq to zero as we are in a new time slice. By doing
                // this we minimize the chance of it reaching 8192 during the slice.                
                _clockSeq = 0;

                // Record the new time slice that we're now working in
                _lastTime = time;

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "_lastTime = " + _lastTime);
            }

            ourSeq = _clockSeq;
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "ourSeq = " + ourSeq);

        /*
         * Bit manipulations and shuffling obtained data into the UUID structure
         */
        _uuidHigh = _CURRENT_VERSION_ID // Top four bits are the version number (0011)
                    + (time & 0x0FFFFFFFFFFFFFFFL) // Rest of timestamp
        ;

        _uuidLow = 0xE000000000000000L // Top three bits are the reserved field (111)
                   | ((ourSeq & 0x00001FFFL) << 48) // Next thirteen bits from the clockseq field
                   | ((long) _PID << 32) // Next sixteen bits are our (so-called) pid
        ;

        // Add the IP Address into the UUID
        _uuidLow = _uuidLow | _IP_ADDRESS;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "UUID", this);
    }

    /* ------------------------------------------------------------------------ */
    /* UUID method */
    /* ------------------------------------------------------------------------ */
    /**
     * (Re)construct the uuid from an array of bytes. In this case it is
     * up to the user of the constructor to ensure that the UUID is still used
     * correctly if used to identify unique object etc.
     * 
     * @author David Vines
     * 
     * @param bytes The 16 byte array from which to reconstruct the uuid
     */
    /* ------------------------------------------------------------------------ */
    public UUID(byte[] bytes)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "UUID", com.ibm.ejs.util.Util.toHexString(bytes));

        if (bytes == null)
        {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "UUID", "NullPointerException");
            throw new NullPointerException();
        }

        if (bytes.length != 16)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected format byte[] length != 16");

            // The bytes data is not in the expected format.
            // Tolerate this by storing the byte[] in
            // _cachedBytes and initialize _cachedString
            // to be the hex form of the byte[] data.

            _cachedBytes = new byte[bytes.length];
            System.arraycopy(bytes, 0, _cachedBytes, 0, _cachedBytes.length);

            _cachedString = com.ibm.ejs.util.Util.toHexString(bytes);

            _unexpectedFormat = true;
        }
        else
        {
            // clone the array for safety & immutability
            byte[] cloned = new byte[16];
            System.arraycopy(bytes, 0, cloned, 0, 16);

            fillFromBytes(cloned);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "UUID", this);
    }

    /* ------------------------------------------------------------------------ */
    /* UUID method */
    /* ------------------------------------------------------------------------ */
    /**
     * (Re)construct from the string representation. Again it is up to the
     * user of the constructor to ensure that the UUID is used corrected
     * 
     * @author David Vines
     * 
     * @param string The string form of the uuid
     */
    /* ------------------------------------------------------------------------ */
    public UUID(String string)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "UUID", string);

        byte[] bytes = new byte[16];
        bytes[0] = (byte) Integer.parseInt(string.substring(14, 16), 16);
        bytes[1] = (byte) Integer.parseInt(string.substring(16, 18), 16);
        bytes[2] = (byte) Integer.parseInt(string.substring(9, 11), 16);
        bytes[3] = (byte) Integer.parseInt(string.substring(11, 13), 16);
        bytes[4] = (byte) Integer.parseInt(string.substring(0, 2), 16);
        bytes[5] = (byte) Integer.parseInt(string.substring(2, 4), 16);
        bytes[6] = (byte) Integer.parseInt(string.substring(4, 6), 16);
        bytes[7] = (byte) Integer.parseInt(string.substring(6, 8), 16);
        bytes[8] = (byte) Integer.parseInt(string.substring(19, 21), 16);
        bytes[9] = (byte) Integer.parseInt(string.substring(21, 23), 16);
        bytes[10] = (byte) Integer.parseInt(string.substring(24, 26), 16);
        bytes[11] = (byte) Integer.parseInt(string.substring(26, 28), 16);
        bytes[12] = (byte) Integer.parseInt(string.substring(28, 30), 16);
        bytes[13] = (byte) Integer.parseInt(string.substring(30, 32), 16);
        bytes[14] = (byte) Integer.parseInt(string.substring(32, 34), 16);
        bytes[15] = (byte) Integer.parseInt(string.substring(34, 36), 16);

        fillFromBytes(bytes);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "UUID", this);
    }

    /* ------------------------------------------------------------------------ */
    /* fillFromBytes method */
    /* ------------------------------------------------------------------------ */
    /**
     * Complete construction of the uuid from the byte array form of the
     * uuid
     * 
     * @author David Vines
     * 
     * @param bytes The byte array version of the uuid
     */
    /* ------------------------------------------------------------------------ */
    protected void fillFromBytes(byte[] bytes)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fillFromBytes", com.ibm.ejs.util.Util.toHexString(bytes));

        _uuidHigh = (((long) bytes[0]) & 0xFF) << 56
                    | (((long) bytes[1]) & 0xFF) << 48
                    | (((long) bytes[2]) & 0xFF) << 40
                    | (((long) bytes[3]) & 0xFF) << 32
                    | (((long) bytes[4]) & 0xFF) << 24
                    | (((long) bytes[5]) & 0xFF) << 16
                    | (((long) bytes[6]) & 0xFF) << 8
                    | (((long) bytes[7]) & 0xFF) << 0;
        _uuidLow = (((long) bytes[8]) & 0xFF) << 56
                   | (((long) bytes[9]) & 0xFF) << 48
                   | (((long) bytes[10]) & 0xFF) << 40
                   | (((long) bytes[11]) & 0xFF) << 32
                   | (((long) bytes[12]) & 0xFF) << 24
                   | (((long) bytes[13]) & 0xFF) << 16
                   | (((long) bytes[14]) & 0xFF) << 8
                   | (((long) bytes[15]) & 0xFF) << 0;

        _cachedBytes = bytes;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fillFromBytes");
    }

    /* ------------------------------------------------------------------------ */
    /* toString method */
    /* ------------------------------------------------------------------------ */
    /**
     * Convert the uuid into a string
     * 
     * @author David Vines
     * 
     * @return String Description of returned value
     */
    /* ------------------------------------------------------------------------ */
    @Override
    public String toString()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toString");

        // Just in case you're wondering, if two threads race to create the
        // cached strings, we may end up with two identical strings, one of which
        // is cached, while the other thread uses the second. Since the strings are
        // identical, this does not cause a problem!
        if (_cachedString == null)
        {
            /* Format the string. */
            char[] string = new char[36];

            string[0] = _NIBBLE[(int) ((_uuidHigh & 0x00000000F0000000L) >>> 28)];
            string[1] = _NIBBLE[(int) ((_uuidHigh & 0x000000000F000000L) >>> 24)];
            string[2] = _NIBBLE[(int) ((_uuidHigh & 0x0000000000F00000L) >>> 20)];
            string[3] = _NIBBLE[(int) ((_uuidHigh & 0x00000000000F0000L) >>> 16)];
            string[4] = _NIBBLE[(int) ((_uuidHigh & 0x000000000000F000L) >>> 12)];
            string[5] = _NIBBLE[(int) ((_uuidHigh & 0x0000000000000F00L) >>> 8)];
            string[6] = _NIBBLE[(int) ((_uuidHigh & 0x00000000000000F0L) >>> 4)];
            string[7] = _NIBBLE[(int) ((_uuidHigh & 0x000000000000000FL) >>> 0)];
            string[8] = '-';
            string[9] = _NIBBLE[(int) ((_uuidHigh & 0x0000F00000000000L) >>> 44)];
            string[10] = _NIBBLE[(int) ((_uuidHigh & 0x00000F0000000000L) >>> 40)];
            string[11] = _NIBBLE[(int) ((_uuidHigh & 0x000000F000000000L) >>> 36)];
            string[12] = _NIBBLE[(int) ((_uuidHigh & 0x0000000F00000000L) >>> 32)];
            string[13] = '-';
            string[14] = _NIBBLE[(int) ((_uuidHigh & 0xF000000000000000L) >>> 60)];
            string[15] = _NIBBLE[(int) ((_uuidHigh & 0x0F00000000000000L) >>> 56)];
            string[16] = _NIBBLE[(int) ((_uuidHigh & 0x00F0000000000000L) >>> 52)];
            string[17] = _NIBBLE[(int) ((_uuidHigh & 0x000F000000000000L) >>> 48)];
            string[18] = '-';
            string[19] = _NIBBLE[(int) ((_uuidLow & 0xF000000000000000L) >>> 60)];
            string[20] = _NIBBLE[(int) ((_uuidLow & 0x0F00000000000000L) >>> 56)];
            string[21] = _NIBBLE[(int) ((_uuidLow & 0x00F0000000000000L) >>> 52)];
            string[22] = _NIBBLE[(int) ((_uuidLow & 0x000F000000000000L) >>> 48)];
            string[23] = '-';
            string[24] = _NIBBLE[(int) ((_uuidLow & 0x0000F00000000000L) >>> 44)];
            string[25] = _NIBBLE[(int) ((_uuidLow & 0x00000F0000000000L) >>> 40)];
            string[26] = _NIBBLE[(int) ((_uuidLow & 0x000000F000000000L) >>> 36)];
            string[27] = _NIBBLE[(int) ((_uuidLow & 0x0000000F00000000L) >>> 32)];
            string[28] = _NIBBLE[(int) ((_uuidLow & 0x00000000F0000000L) >>> 28)];
            string[29] = _NIBBLE[(int) ((_uuidLow & 0x000000000F000000L) >>> 24)];
            string[30] = _NIBBLE[(int) ((_uuidLow & 0x0000000000F00000L) >>> 20)];
            string[31] = _NIBBLE[(int) ((_uuidLow & 0x00000000000F0000L) >>> 16)];
            string[32] = _NIBBLE[(int) ((_uuidLow & 0x000000000000F000L) >>> 12)];
            string[33] = _NIBBLE[(int) ((_uuidLow & 0x0000000000000F00L) >>> 8)];
            string[34] = _NIBBLE[(int) ((_uuidLow & 0x00000000000000F0L) >>> 4)];
            string[35] = _NIBBLE[(int) ((_uuidLow & 0x000000000000000FL) >>> 0)];

            _cachedString = new String(string);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toString", _cachedString);

        return _cachedString;
    }

    /* ------------------------------------------------------------------------ */
    /* equals method */
    /* ------------------------------------------------------------------------ */
    /**
     * Test if some other object is equal to this uuid
     * 
     * @author David Vines
     * 
     * @param other The other object to be tested
     * @return boolean Return true if the other object is equal to this one
     */
    /* ------------------------------------------------------------------------ */
    @Override
    public boolean equals(Object object)
    {
        if (this == object)
            return true;
        if (object == null)
            return false;
        if (object.getClass() != getClass())
            return false;

        final UUID other = (UUID) object;

        if (_unexpectedFormat)
        {
            // This UUID was constructed from a byte[] of
            // an unexpected format. _uuidHigh and _uuidLow
            // have not been initialized so instead we perform
            // an equality check on the UUIDs' byte array forms

            return Arrays.equals(_cachedBytes, other.toByteArray());
        }

        if (_uuidHigh != other._uuidHigh)
            return false;
        if (_uuidLow != other._uuidLow)
            return false;

        return true;
    }

    /* ------------------------------------------------------------------------ */
    /* hashCode method */
    /* ------------------------------------------------------------------------ */
    /**
     * Return the hashcode of this object. Note that if two objects are equals()
     * each other, their hashCodes must be the same
     * 
     * @author David Vines
     * 
     * @return int Description of returned value
     */
    /* ------------------------------------------------------------------------ */
    @Override
    public int hashCode()
    {
        if (_unexpectedFormat)
        {
            if (!_hashed)
            {
                for (int i = 0; i < _cachedBytes.length; i++)
                {
                    _hashCode += _cachedBytes[i];
                }

                _hashed = true;
            }

            return _hashCode;
        }
        else
        {
            return (int) _uuidHigh;
        }
    }

    /* ------------------------------------------------------------------------ */
    /* toByteArray method */
    /* ------------------------------------------------------------------------ */
    /**
     * Convert the UUID into a byte array
     * 
     * @author David Vines
     * 
     * @return byte[] A byte array version of the uuid
     */
    /* ------------------------------------------------------------------------ */
    public byte[] toByteArray()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toByteArray");

        // Just in case you're wondering, if two threads race to create the
        // cached arrays, we may end up with two identical arrays, one of which
        // is cached, while the other thread uses the second. Since the contents of
        // the arrays are identical, this does not cause a problem!
        if (_cachedBytes == null)
        {
            _cachedBytes = new byte[16];

            _cachedBytes[0] = (byte) ((_uuidHigh & 0xFF00000000000000L) >>> 56);
            _cachedBytes[1] = (byte) ((_uuidHigh & 0x00FF000000000000L) >>> 48);
            _cachedBytes[2] = (byte) ((_uuidHigh & 0x0000FF0000000000L) >>> 40);
            _cachedBytes[3] = (byte) ((_uuidHigh & 0x000000FF00000000L) >>> 32);
            _cachedBytes[4] = (byte) ((_uuidHigh & 0x00000000FF000000L) >>> 24);
            _cachedBytes[5] = (byte) ((_uuidHigh & 0x0000000000FF0000L) >>> 16);
            _cachedBytes[6] = (byte) ((_uuidHigh & 0x000000000000FF00L) >>> 8);
            _cachedBytes[7] = (byte) ((_uuidHigh & 0x00000000000000FFL) >>> 0);
            _cachedBytes[8] = (byte) ((_uuidLow & 0xFF00000000000000L) >>> 56);
            _cachedBytes[9] = (byte) ((_uuidLow & 0x00FF000000000000L) >>> 48);
            _cachedBytes[10] = (byte) ((_uuidLow & 0x0000FF0000000000L) >>> 40);
            _cachedBytes[11] = (byte) ((_uuidLow & 0x000000FF00000000L) >>> 32);
            _cachedBytes[12] = (byte) ((_uuidLow & 0x00000000FF000000L) >>> 24);
            _cachedBytes[13] = (byte) ((_uuidLow & 0x0000000000FF0000L) >>> 16);
            _cachedBytes[14] = (byte) ((_uuidLow & 0x000000000000FF00L) >>> 8);
            _cachedBytes[15] = (byte) ((_uuidLow & 0x00000000000000FFL) >>> 0);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toByteArray", com.ibm.ejs.util.Util.toHexString(_cachedBytes));

        return _cachedBytes;
    }

    /* ------------------------------------------------------------------------ */
    /* compareTo method */
    /* ------------------------------------------------------------------------ */
    /**
     * Compare this UUID to another object. If this object becomes before
     * the other object return a negative number, if this object is equal
     * to the other object object and if this object comes after the other
     * object return a positive number
     * 
     * @author David Vines
     * 
     * @param obj The other object with which to compare this object
     * @return int A negative integer if this object is less than obj, 0 if this
     *         object is equals to obj and a positive integer if this object is
     *         greater than obj
     */
    /* ------------------------------------------------------------------------ */
    @Override
    public int compareTo(Object obj)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "compareTo", obj);

        UUID other = (UUID) obj; // Allowed to throw ClassCastException

        int result = 0;

        if (equals(other))
        {
            result = 0;
        }
        else if (_unexpectedFormat)
        {
            // This UUID is in an unexpected format so we do not have
            // uuidHigh and uuidLow to compare. Instead we compare the
            // two UUID's hash codes (the sum of each byte of their
            // byte array form).
            result = Integer.valueOf(hashCode()).compareTo(Integer.valueOf(other.hashCode()));
        }
        else
        {
            result = Long.valueOf(_uuidHigh).compareTo(Long.valueOf(other._uuidHigh));

            if (result == 0)
            {
                result = Long.valueOf(_uuidLow).compareTo(Long.valueOf(other._uuidLow));
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "compareTo", Integer.valueOf(result));
        return result;
    }

    /* ---------------------------------------------------------------------- */
    /* clone method */
    /* ---------------------------------------------------------------------- */
    /**
     * Creates a copy of this object.
     * 
     * @return Object new utility object
     */
    /* ---------------------------------------------------------------------- */
    @Override
    public Object clone()
    {
        // Since this UUID is immutable, we can return ourselves as the clone
        return this;
    }

    /* ------------------------------------------------------------------------ */
    /* isCurrentVersion method */
    /* ------------------------------------------------------------------------ */
    /**
     * Determine if this UUID was created with the current version
     * 
     * @author David Vines
     * 
     * @return boolean true if this UUID was created with the current version
     */
    /* ------------------------------------------------------------------------ */
    public boolean isCurrentVersion()
    {
        if (_unexpectedFormat)
        {
            return false;
        }
        else
        {
            return ((_uuidHigh & _VERSION_MASK) == _CURRENT_VERSION_ID);
        }
    }

    /* ------------------------------------------------------------------------ */
    /* isOldVersion method */
    /* ------------------------------------------------------------------------ */
    /**
     * Determine if this UUID was created with an old (but recognised) version
     * 
     * @author David Vines
     * 
     * @return boolean true if this UUID was created with an old (but recognised) version
     * 
     */
    /* ------------------------------------------------------------------------ */
    public boolean isOldVersion()
    {
        if (_unexpectedFormat)
        {
            return false;
        }
        else
        {
            final long version = _uuidHigh & _VERSION_MASK;

            return (version >= _OLD_VERSION_ID && version < _CURRENT_VERSION_ID);
        }
    }

    /* ------------------------------------------------------------------------ */
    /* getPID method */
    /* ------------------------------------------------------------------------ */
    /**
     * Return a 16-bit unique identifier for this classloader. The identifier
     * needs to be unique being all class loaders on this IP address (i.e. the
     * uniqueness needs to extend between two JVMs on the same IP address).
     * 
     * <p><b>Note:</b>In an environment where RAS is available - server, J2EE
     * client environment we can correctly ascertain the PID. However in any other
     * environment, e.g. thin client we have no good way to do this so we get
     * a random number and hope....)
     * 
     * @author David Vines
     * 
     * @return short The process's PID or a random value if the PID could not
     *         be determined
     */
    /* ------------------------------------------------------------------------ */
    private static int getPID()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getPID");

        short spid = 0;
        int ipid = 0;

        try
        {
            spid = Short.parseShort(com.ibm.ejs.ras.RasHelper.getProcessId());
        } catch (Throwable t)
        {
            // No FFDC code needed.

            if (tc.isEventEnabled())
                Tr.event(tc, "Caught throwable parsing PID", t);
            spid = (short) (new java.util.Random().nextInt() & 0x0000FFFF); // (i.e. keep 16 bits)
        }

        // Convert the spid value (a 16 bit short that could be posivive or negative)
        // into an integer without allowing java to perform sign extension. This is required
        // as the value will be bit-wise ORed onto a larger structure during the
        // generation of individual UUIDs. If the number was negative then the sign extension
        // would cause the two most significant bytes to be FF and this would damage the
        // larger structure (these bytes MUST be 00)
        ipid = (spid & 0x0000FFFF);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getPID", Integer.valueOf(ipid));

        return ipid;
    }

    // This method gets the machine's IP address and maps
    // it into a long (64-bits). This is performed by
    // mapping the components a.b.c.d of the IP address
    // into an int and using this to comprise the lower 32 bits
    // of the long. The upper 32 bits are all set to zero
    // to ensure that the returned value can be safely bitwise
    // ORed into the UUID.
    //
    // The lower 32-bits are comprised thus:
    //
    // The most significant 8 bits defined by a, the next
    // 8 bits by b, the next 8 bits by c, and finally the
    // least significant 8 bits by d.
    //
    // For example an IP address of 9.20.217.12:
    //
    //   9 = 00001001
    //  20 = 00010100
    // 217 = 11011001
    //  12 = 00001100
    //
    // This is manipulated to become:
    //
    // |--9---| |--20--| |-217--| |--12--|
    // 00001001 00010100 11011001 00001100
    //
    // Which as a base 10 int is 152361228  - the method's
    // return value when the IP address is 9.20.217.12 
    private static long getIPAddress()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getIPAddress");

        int lower32Bits = 0;

        try
        {
            final byte[] address = AccessController.doPrivileged(new PrivilegedExceptionAction<byte[]>() {
                @Override
                public byte[] run() throws UnknownHostException {
                    return java.net.InetAddress.getLocalHost().getAddress();

                }
            });

            lower32Bits = ((address[0] & 0xFF) << 24)
                          | ((address[1] & 0xFF) << 16)
                          | ((address[2] & 0xFF) << 8)
                          | ((address[3] & 0xFF) << 0);

        } catch (PrivilegedActionException paex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(paex.getCause(), "com.ibm.ws.util.UUID.getIPAddress", "794");
            if (tc.isEventEnabled())
                Tr.event(tc, "Exception caught getting host address.", paex.getCause());

            // Try random number instead!
            lower32Bits = new java.util.Random().nextInt();
        } catch (Exception e) // Host address unavailable
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.util.UUID.getIPAddress", "661");
            if (tc.isEventEnabled())
                Tr.event(tc, "Exception caught getting host address.", e);

            // Try random number instead!
            lower32Bits = new java.util.Random().nextInt();
        }

        final long ipAddress = lower32Bits & 0x00000000FFFFFFFFL;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getIPAddress", Long.valueOf(ipAddress));
        return ipAddress;
    }

    // Return the equivalent of System.currentTimeMillis, but
    // using System.nanoTime to determine the time interval between
    // time this class was loaded and now - this avoids the clock
    // going backwards (e.g. due to NTP) while this class is active.
    //
    // Of course, between different class loaders (and JVMs) the time
    // could go backwards, but this approach stops NTP from causing
    // duplicate UUID during a run
    private static long getTime()
    {
        return _CLASS_LOAD_SYSTEM_TIME + (System.nanoTime() - _CLASS_LOAD_NANO_TIME) / NANOS_IN_A_MILLISECOND;
    }
}
