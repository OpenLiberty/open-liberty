/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.cache;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.session.store.cache.TypeConversion;

public class SerializationInfoCache {    

    public static BuiltinSerializationInfo<?> lookupByClass(Class<?> c) {
        return BUILTIN_MAPPINGS.get(c);
    }

    public static BuiltinSerializationInfo<?> lookupByIndex(byte index) {
        BuiltinSerializationInfo<?> returnValue = null;
        if (index < BUILTIN_SIZE && index > -1) {
            returnValue = BUILTIN_ARRAY[index];
        }
        return returnValue;
    }

    public static final byte LONG = 0;
    public static final BuiltinSerializationInfo<Long> LONG_INFO = new LongInfo();

    public static final byte INTEGER = 1;
    public static final BuiltinSerializationInfo<Integer> INTEGER_INFO = new IntegerInfo();

    public static final byte SHORT = 2;
    public static final BuiltinSerializationInfo<Short> SHORT_INFO = new ShortInfo();

    public static final byte BYTE = 3;
    public static final BuiltinSerializationInfo<Byte> BYTE_INFO = new ByteInfo();

    public static final byte FLOAT = 4;
    public static final BuiltinSerializationInfo<Float> FLOAT_INFO = new FloatInfo();

    public static final byte DOUBLE = 5;
    public static final BuiltinSerializationInfo<Double> DOUBLE_INFO = new DoubleInfo();

    public static final byte CHARACTER = 6;
    public static final BuiltinSerializationInfo<Character> CHARACTER_INFO = new CharacterInfo();

    public static final byte BOOLEAN = 7;
    public static final BuiltinSerializationInfo<Boolean> BOOLEAN_INFO = new BooleanInfo();

    public static final byte DATE = 8;
    public static final BuiltinSerializationInfo<java.util.Date> DATE_INFO = new DateInfo();

    public static final byte SQL_DATE = 9;
    public static final BuiltinSerializationInfo<java.sql.Date> SQL_DATE_INFO = new SqlDateInfo();

    public static final byte SQL_TIMESTAMP = 10;
    public static final BuiltinSerializationInfo<Timestamp> SQL_TIMESTAMP_INFO = new SqlTimestampInfo();

    public static final byte SQL_TIME = 11;
    public static final BuiltinSerializationInfo<Time> SQL_TIME_INFO = new SqlTimeInfo();

    public static final byte BIG_DECIMAL = 12;
    public static final BuiltinSerializationInfo<BigDecimal> BIG_DECIMAL_INFO = new BigDecimalInfo();

    public static final byte BIG_INTEGER = 13;
    public static final BuiltinSerializationInfo<BigInteger> BIG_INTEGER_INFO = new BigIntegerInfo();

    public static final byte BYTE_ARRAY = 14;
    public static final BuiltinSerializationInfo<byte[]> BYTE_ARRAY_INFO = new ByteArrayInfo();

    public static final byte ATOMIC_INTEGER = 15;
    public static final BuiltinSerializationInfo<AtomicInteger> ATOMIC_INTEGER_INFO = new AtomicIntegerInfo();

    public static final byte ATOMIC_LONG = 16;
    public static final BuiltinSerializationInfo<AtomicLong> ATOMIC_LONG_INFO = new AtomicLongInfo();

    public static final byte BUILTIN_SIZE = 17;
    
    public static final byte BUILTIN_SERIALIZATION = 0;

    private static final Map<Class<?>, BuiltinSerializationInfo<?>> BUILTIN_MAPPINGS = new HashMap<Class<?>, BuiltinSerializationInfo<?>>();
    private static final BuiltinSerializationInfo<?>[] BUILTIN_ARRAY = new BuiltinSerializationInfo[BUILTIN_SIZE];

    private static void addBuiltinSerializationInfo(BuiltinSerializationInfo<?> info) {
        BUILTIN_MAPPINGS.put(info.getObjectClass(), info);
        BUILTIN_ARRAY[info.getIndex()] = info;
    }

    static {
        addBuiltinSerializationInfo(LONG_INFO);
        addBuiltinSerializationInfo(INTEGER_INFO);
        addBuiltinSerializationInfo(SHORT_INFO);
        addBuiltinSerializationInfo(BYTE_INFO);
        addBuiltinSerializationInfo(FLOAT_INFO);
        addBuiltinSerializationInfo(DOUBLE_INFO);
        addBuiltinSerializationInfo(CHARACTER_INFO);
        addBuiltinSerializationInfo(BOOLEAN_INFO);
        addBuiltinSerializationInfo(DATE_INFO);
        addBuiltinSerializationInfo(SQL_DATE_INFO);
        addBuiltinSerializationInfo(SQL_TIMESTAMP_INFO);
        addBuiltinSerializationInfo(SQL_TIME_INFO);
        addBuiltinSerializationInfo(BIG_DECIMAL_INFO);
        addBuiltinSerializationInfo(BIG_INTEGER_INFO);
        addBuiltinSerializationInfo(BYTE_ARRAY_INFO);
        addBuiltinSerializationInfo(ATOMIC_INTEGER_INFO);
        addBuiltinSerializationInfo(ATOMIC_LONG_INFO);
    }

    @Trivial // reveals customer data
    public static final class LongInfo implements BuiltinSerializationInfo<Long> {
        public byte getIndex() {
            return LONG;
        }

        public Class<Long> getObjectClass() {
            return Long.class;
        }

        public Long bytesToObject(byte[] bytes) {
            return Long.valueOf(TypeConversion.varIntBytesToLong(bytes, 2));
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, LONG, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeLongAsVarIntBytes(((Long)object).longValue(), bytes, 2);
            if(numWritten < 12) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class IntegerInfo implements BuiltinSerializationInfo<Integer> {
        public byte getIndex() {
            return INTEGER;
        }

        public Class<Integer> getObjectClass() {
            return Integer.class;
        }

        public Integer bytesToObject(byte[] bytes) {
            return Integer.valueOf(TypeConversion.varIntBytesToInt(bytes, 2));
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, INTEGER, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeIntAsVarIntBytes(((Integer)object).intValue(), bytes, 2);
            if(numWritten < 7) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class ShortInfo implements BuiltinSerializationInfo<Short> {
        public byte getIndex() {
            return SHORT;
        }

        public Class<Short> getObjectClass() {
            return Short.class;
        }

        public Short bytesToObject(byte[] bytes) {
            return Short.valueOf(TypeConversion.bytesToShort(bytes, 2));
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, SHORT, 0, 0};
            TypeConversion.shortToBytes(((Short)object).shortValue(), bytes, 2);
            return bytes;
        }

    }

    @Trivial // reveals customer data
    public static final class ByteInfo implements BuiltinSerializationInfo<Byte> {
        public byte getIndex() {
            return BYTE;
        }

        public Class<Byte> getObjectClass() {
            return Byte.class;
        }
        
        public Byte bytesToObject(byte[] bytes) {
            return Byte.valueOf(bytes[2]);
        }

        public byte[] objectToBytes(Object object) {
            byte b = ((Byte) object).byteValue();
            return new byte[] {BUILTIN_SERIALIZATION, BYTE, b};
        }
    }

    @Trivial // reveals customer data
    public static final class FloatInfo implements BuiltinSerializationInfo<Float> {
        public byte getIndex() {
            return FLOAT;
        }

        public Class<Float> getObjectClass() {
            return Float.class;
        }

        public Float bytesToObject(byte[] bytes) {
            int floatBits = TypeConversion.varIntBytesToInt(bytes, 2);
            return new Float(Float.intBitsToFloat(floatBits));
        }

        public byte[] objectToBytes(Object object) {
            float f = ((Float) object).floatValue();
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, FLOAT, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeIntAsVarIntBytes(Float.floatToIntBits(f), bytes, 2);
            if(numWritten < 7) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class DoubleInfo implements BuiltinSerializationInfo<Double> {
        public byte getIndex() {
            return DOUBLE;
        }

        public Class<Double> getObjectClass() {
            return Double.class;
        }

        public Double bytesToObject(byte[] bytes) {
            long doubleBits = TypeConversion.varIntBytesToLong(bytes, 2);
            return new Double(Double.longBitsToDouble(doubleBits));
        }

        public byte[] objectToBytes(Object object) {
            double d = ((Double) object).doubleValue();
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, DOUBLE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeLongAsVarIntBytes(Double.doubleToLongBits(d), bytes, 2);
            if(numWritten < 12) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class CharacterInfo implements BuiltinSerializationInfo<Character> {
        public byte getIndex() {
            return CHARACTER;
        }

        public Class<Character> getObjectClass() {
            return Character.class;
        }

        public Character bytesToObject(byte[] bytes) {
            return Character.valueOf(TypeConversion.bytesToChar(bytes, 2));
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, CHARACTER, 0, 0};
            TypeConversion.charToBytes(((Character)object).charValue(), bytes, 2);
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class BooleanInfo implements BuiltinSerializationInfo<Boolean> {
        private static final byte[] TRUE_BYTES = {BUILTIN_SERIALIZATION, BOOLEAN, 1};
        private static final byte[] FALSE_BYTES = {BUILTIN_SERIALIZATION, BOOLEAN, 0};

        public byte getIndex() {
            return BOOLEAN;
        }

        public Class<Boolean> getObjectClass() {
            return Boolean.class;
        }

        public Boolean bytesToObject(byte[] bytes) {
            return bytes[2] == 0 ? Boolean.FALSE : Boolean.TRUE;
        }

        public byte[] objectToBytes(Object object) {
            return ((Boolean)object).booleanValue() ? TRUE_BYTES : FALSE_BYTES;
        }
    }

    @Trivial // reveals customer data
    public static final class DateInfo implements BuiltinSerializationInfo<java.util.Date> {
        public byte getIndex() {
            return DATE;
        }

        public Class<java.util.Date> getObjectClass() {
            return java.util.Date.class;
        }

        public java.util.Date bytesToObject(byte[] bytes) {
            long l = TypeConversion.varIntBytesToLong(bytes, 2);
            return new java.util.Date(l);
        }

        public byte[] objectToBytes(Object object) {
            long l = ((java.util.Date)object).getTime();
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, DATE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeLongAsVarIntBytes(l, bytes, 2);
            if(numWritten < 12) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class SqlDateInfo implements BuiltinSerializationInfo<java.sql.Date> {
        public byte getIndex() {
            return SQL_DATE;
        }

        public Class<java.sql.Date> getObjectClass() {
            return java.sql.Date.class;
        }

        public java.sql.Date bytesToObject(byte[] bytes) {
            long l = TypeConversion.varIntBytesToLong(bytes, 2);
            return new java.sql.Date(l);
        }

        public byte[] objectToBytes(Object object) {
            long l = ((java.sql.Date)object).getTime();
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, SQL_DATE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeLongAsVarIntBytes(l, bytes, 2);
            if(numWritten < 12) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class SqlTimestampInfo implements BuiltinSerializationInfo<Timestamp> {
        public byte getIndex() {
            return SQL_TIMESTAMP;
        }

        public Class<Timestamp> getObjectClass() {
            return Timestamp.class;
        }

        public Timestamp bytesToObject(byte[] bytes) {
            long l = TypeConversion.bytesToLong(bytes, 2);
            int nanos = TypeConversion.bytesToInt(bytes, 10);
            Timestamp returnValue = new Timestamp(l);
            returnValue.setNanos(nanos);
            return returnValue;
        }

        public byte[] objectToBytes(Object object) {
            Timestamp time = (Timestamp)object;
            long l = time.getTime();
            int nanos = time.getNanos();
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, SQL_TIMESTAMP, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            TypeConversion.longToBytes(l, bytes, 2);
            TypeConversion.intToBytes(nanos, bytes, 10);
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class SqlTimeInfo implements BuiltinSerializationInfo<Time> {
        public byte getIndex() {
            return SQL_TIME;
        }

        public Class<Time> getObjectClass() {
            return Time.class;
        }

        public Time bytesToObject(byte[] bytes) {
            long l = TypeConversion.varIntBytesToLong(bytes, 2);
            return new Time(l);
        }

        public byte[] objectToBytes(Object object) {
            long l = ((Time)object).getTime();
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, SQL_TIME, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeLongAsVarIntBytes(l, bytes, 2);
            if(numWritten < 12) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class BigDecimalInfo implements BuiltinSerializationInfo<BigDecimal> {
        public byte getIndex() {
            return BIG_DECIMAL;
        }

        public Class<BigDecimal> getObjectClass() {
            return BigDecimal.class;
        }

        public BigDecimal bytesToObject(byte[] bytes) {
            int length = TypeConversion.bytesToInt(bytes, 2);
            byte[] decimalBytes = new byte[length];
            System.arraycopy(bytes, 6, decimalBytes, 0, length);
            int scale = TypeConversion.bytesToInt(bytes, length + 6);
            return new BigDecimal(new BigInteger(decimalBytes), scale);
        }

        public byte[] objectToBytes(Object object) {
            BigDecimal bd = (BigDecimal) object;
            BigInteger bi = bd.unscaledValue();
            byte[] decimalBytes = bi.toByteArray();
            int length = decimalBytes.length;

            byte[] bytes = new byte[length + 10];
            bytes[0] = BUILTIN_SERIALIZATION;
            bytes[1] = BIG_DECIMAL;

            TypeConversion.intToBytes(length, bytes, 2);
            System.arraycopy(decimalBytes, 0, bytes, 6, length);
            TypeConversion.intToBytes(bd.scale(), bytes, length + 6);

            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class BigIntegerInfo implements BuiltinSerializationInfo<BigInteger> {
        public byte getIndex() {
            return BIG_INTEGER;
        }

        public Class<BigInteger> getObjectClass() {
            return BigInteger.class;
        }

        public BigInteger bytesToObject(byte[] bytes) {
            int length = TypeConversion.bytesToInt(bytes, 2);
            byte[] integerBytes = new byte[length];
            System.arraycopy(bytes, 6, integerBytes, 0, length);
            return new BigInteger(integerBytes);
        }

        public byte[] objectToBytes(Object object) {
            BigInteger bi = (BigInteger) object;
            byte[] integerBytes = bi.toByteArray();
            int length = integerBytes.length;

            byte[] bytes = new byte[length + 6];
            bytes[0] = BUILTIN_SERIALIZATION;
            bytes[1] = BIG_INTEGER;

            TypeConversion.intToBytes(length, bytes, 2);
            System.arraycopy(integerBytes, 0, bytes, 6, length);

            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class ByteArrayInfo implements BuiltinSerializationInfo<byte[]> {
        public byte getIndex() {
            return BYTE_ARRAY;
        }

        public Class<byte[]> getObjectClass() {
            return byte[].class;
        }

        public byte[] bytesToObject(byte[] bytes) {
            int length = TypeConversion.bytesToInt(bytes, 2);
            byte[] returnBytes = new byte[length];
            System.arraycopy(bytes, 6, returnBytes, 0, length);
            return returnBytes;
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = (byte[]) object;
            int length = bytes.length;

            byte[] returnBytes = new byte[length + 6];
            returnBytes[0] = BUILTIN_SERIALIZATION;
            returnBytes[1] = BYTE_ARRAY;

            TypeConversion.intToBytes(length, returnBytes, 2);
            System.arraycopy(bytes, 0, returnBytes, 6, length);

            return returnBytes;
        }
    }
    
    @Trivial // reveals customer data
    public static final class AtomicIntegerInfo implements BuiltinSerializationInfo<AtomicInteger> {
        public byte getIndex() {
            return ATOMIC_INTEGER;
        }

        public Class<AtomicInteger> getObjectClass() {
            return AtomicInteger.class;
        }

        public AtomicInteger bytesToObject(byte[] bytes) {
            int atomicIntValue = TypeConversion.varIntBytesToInt(bytes, 2);
            return new AtomicInteger(atomicIntValue);
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, ATOMIC_INTEGER, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeIntAsVarIntBytes(((AtomicInteger)object).get(), bytes, 2);
            if(numWritten < 7) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }

    @Trivial // reveals customer data
    public static final class AtomicLongInfo implements BuiltinSerializationInfo<AtomicLong> {
        public byte getIndex() {
            return ATOMIC_LONG;
        }

        public Class<AtomicLong> getObjectClass() {
            return AtomicLong.class;
        }

        public AtomicLong bytesToObject(byte[] bytes) {
            long atomicLongValue = TypeConversion.varIntBytesToLong(bytes, 2);
            return new AtomicLong(atomicLongValue);
        }

        public byte[] objectToBytes(Object object) {
            byte[] bytes = new byte[] {BUILTIN_SERIALIZATION, ATOMIC_LONG, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int numWritten = TypeConversion.writeLongAsVarIntBytes(((AtomicLong)object).get(), bytes, 2);
            if(numWritten < 12) {
                byte[] smallArray = new byte[numWritten];
                System.arraycopy(bytes, 0, smallArray, 0, numWritten);
                return smallArray;
            }
            return bytes;
        }
    }
}