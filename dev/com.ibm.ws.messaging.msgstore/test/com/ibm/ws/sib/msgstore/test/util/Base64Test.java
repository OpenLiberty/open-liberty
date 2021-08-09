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
package com.ibm.ws.sib.msgstore.test.util;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 *  SIB0048c.ms.2  02/02/07 gareth   Resolve wide XID problem
 * ============================================================================
 */

import java.lang.reflect.Array;

import java.util.Random;

import com.ibm.ws.sib.msgstore.test.*;
import com.ibm.ws.sib.utils.Base64Utils;

import junit.framework.TestSuite;

public class Base64Test extends MessageStoreTestCase
{
    public Base64Test(String name)
    {
        super(name);

        //turnOnTrace();
    }

    /**
     * This is an Non-persitent test.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(Base64Test.class);

        return suite;
    }

    public void testBase64EncodeDecode()
    {
        print("|-----------------------------------------------------");
        print("| Base64EncodeDecode:");
        print("|--------------------");

        String        encoded;
        byte[]        decoded;
        StringBuilder decodedBytes;

        try
        {
            byte[] allOnes = new byte[]{-1, -1, -1};
            print("|");
            print("|-----------------------------------------------------");
            print("| Encode of:               {  -1        -1       -1   }");
            print("|  - Value in bits       :  11111111 11111111 11111111");
            print("|                           ^--|-^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    -     -      -     -");
            encoded = Base64Utils.encodeBase64(allOnes);
            print("|  - Actual characters   : "+encoded);
            if (!"!----".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(----)");
            }

            print("|");
            print("| Decode of:              {!    -        -        -       -    }");
            print("|  - Value in bits       :  00111111 00111111 00111111 00111111");
            print("|                             ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :        -1         -1         -1");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(allOnes, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(-1 -1 -1)");
            }


            byte[] allZeros = new byte[]{0, 0, 0};
            print("| ");
            print("|-----------------------------------------------------");
            print("| Encode of:               {   0         0        0   }");
            print("|  - Value in bits       :  00000000 00000000 00000000");
            print("|                           ^--|-^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    A     A      A     A");
            encoded = Base64Utils.encodeBase64(allZeros);
            print("|  - Actual characters   : "+encoded);
            if (!"!AAAA".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(AAAA)");
            }

            print("|");
            print("| Decode of:              {!    A        A        A       A    }");
            print("|  - Value in bits       :  00000000 00000000 00000000 00000000");
            print("|                             ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :         0          0          0");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(allZeros, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(0 0 0)");
            }

            
            byte[] oneByte = new byte[]{65};
            print("|");
            print("|-----------------------------------------------------");
            print("| Encode of:               {   65       -         -   }");
            print("|  - Value in bits       :  01000001 -------- --------");
            print("|                           ^--|-^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    Q     Q      =     =");
            encoded = Base64Utils.encodeBase64(oneByte);
            print("|  - Actual characters   : "+encoded);
            if (!"!QQ==".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(QQ==)");
            }

            print("|");
            print("| Decode of:              {!    Q        Q        =       =    }");
            print("|  - Value in bits       :  00010000 00010000 -------- --------");
            print("|                             ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :        65          -          -");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(oneByte, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(65)");
            }


            byte[] twoBytes = new byte[]{59, -83};
            print("|");
            print("|-----------------------------------------------------");
            print("| Encode of:               {   59      -83        -   }");
            print("|  - Value in bits       :  00111011 10101101 --------");
            print("|                           ^--|-^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    O     6      0     =");
            encoded = Base64Utils.encodeBase64(twoBytes);
            print("|  - Actual characters   : "+encoded);
            if (!"!O60=".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(O60=)");
            }

            print("|");
            print("| Decode of:              {!    O        6        0       =    }");
            print("|  - Value in bits       :  00001110 00111010 00110100 --------");
            print("|                             ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :        59         -83         -");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(twoBytes, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(59 -83)");
            }


            byte[] threeBytes = new byte[]{-83, 59, -128};
            print("|");
            print("|-----------------------------------------------------");
            print("| Encode of:               {  -83       59      -128  }");
            print("|  - Value in bits       :  10101101 00111011 10000000");
            print("|                           ^--|-^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    r     T      u     A");
            encoded = Base64Utils.encodeBase64(threeBytes);
            print("|  - Actual characters   : "+encoded);
            if (!"!rTuA".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(rTuA)");
            }

            print("|");
            print("| Decode of:              {!    r        T        u       A    }");
            print("|  - Value in bits       :  00101011 00010011 00101110 00000000");
            print("|                             ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :        -83         59       -128");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(threeBytes, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(-83 59 -128)");
            }


            byte[] fourBytes = new byte[]{59, -128, 35, -83};
            print("|");
            print("|-----------------------------------------------------");
            print("| Encode of:               {   59      -128      35      -83       -         -   }");
            print("|  - Value in bits       :  00111011 10000000 00100011 10101101 -------- --------");
            print("|                           ^--|-^^--|--^^--|--^^-|--^ ^-|--^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    O     4      A     j      r      Q      =     =   ");
            encoded = Base64Utils.encodeBase64(fourBytes);
            print("|  - Actual characters   : "+encoded);
            if (!"!O4AjrQ==".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(O4AjrQ==)");
            }

            print("|");
            print("| Decode of:              {!    O        4        A       j         r        Q        =        =   }");
            print("|  - Value in bits       :  00001110 00111000 00000000 00100011 00101011 00010000 00000000 00000000");
            print("|                             ^----|----^^----|----^^----|----^   ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :         59       -128         35           -83         -          -");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(fourBytes, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(59 -128 35 -83)");
            }


            byte[] fiveBytes = new byte[]{-128, -35, -1, 75, 42};
            print("| ");
            print("|-----------------------------------------------------");
            print("| Encode of:               {  -128     -35       -1       75       42        -   }");
            print("|  - Value in bits       :  10000000 11011101 11111111 01001011 00101010 --------");
            print("|                           ^--|-^^--|--^^--|--^^-|--^ ^-|--^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    g     N      3     -      S      y      o     =   ");
            encoded = Base64Utils.encodeBase64(fiveBytes);
            print("|  - Actual characters   : "+encoded);
            print("| ");
            if (!"!gN3-Syo=".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(gN3-Syo=)");
            }

            print("|");
            print("| Decode of:              {!    g        N        3       -         S        y        o        =   }");
            print("|  - Value in bits       :  00100000 00001101 00110111 00111111 00010010 00110010 00101000 00000000");
            print("|                             ^----|----^^----|----^^----|----^   ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :       -128        -35        -1             75         42         -");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(fiveBytes, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(-128 -35 -1 75 42)");
            }


            byte[] sixBytes = new byte[]{12, 38, 84, -26, 127, -6};
            print("| ");
            print("|-----------------------------------------------------");
            print("| Encode of:               {   12       38       84      -26      127       -6   }");
            print("|  - Value in bits       :  00001100 00100110 01010100 11100110 01111111 11111010");
            print("|                           ^--|-^^--|--^^--|--^^-|--^ ^-|--^^--|--^^--|--^^-|--^");
            print("|  - Expected characters :!    D     C      Z     U      5      n      -     6   ");
            encoded = Base64Utils.encodeBase64(sixBytes);
            print("|  - Actual characters   : "+encoded);
            print("| ");
            if (!"!DCZU5n-6".equals(encoded))
            {
                print("| Characters do not match! !!!FAILED!!!");
                fail("Ouput does not match expected string: Actual("+encoded+") Expected(DCZU5n-6)");
            }

            print("|");
            print("| Decode of:              {!    D        C        Z       U         5        n        -        6   }");
            print("|  - Value in bits       :  00000011 00000010 00011001 00010100 00111001 00100111 00111111 00111010");
            print("|                             ^----|----^^----|----^^----|----^   ^----|----^^----|----^^----|----^");
            print("|  - Expected bytes      :        12          38        84            -26        127        -6");
            decoded = Base64Utils.decodeBase64(encoded);
            decodedBytes = new StringBuilder();
            for (byte b : decoded)
            {
                decodedBytes.append(b);
                decodedBytes.append(" ");
            }
            print("|  - Actual bytes        : "+decodedBytes);
            print("|-----------------------------------------------------");
            if (!byteArraysEqual(sixBytes, decoded))
            {
                print("| Bytes do not match! !!!FAILED!!!");
                fail("Output does not match expected bytes: Actual("+decodedBytes+") Expected(12 38 84 -26 127 -6)");
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: "+t.getMessage());
        }
        finally
        {
            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    public void testRandomEncodeDecode()
    {
        print("|-----------------------------------------------------");
        print("| RandomEncodeDecode:");
        print("|--------------------");

        byte[] bytes;
        String encoded;
        byte[] decoded;

        try
        {
            Random generator = new Random(System.currentTimeMillis());
    
            for (int i=1; i<1000; i++)
            {
                bytes = new byte[i];
    
                // Generate some random bytes.
                generator.nextBytes(bytes);

                encoded = Base64Utils.encodeBase64(bytes);

                // print("| Bytes encoded: "+encoded);

                decoded = Base64Utils.decodeBase64(encoded);

                if (!byteArraysEqual(bytes, decoded))
                {
                    print("| Bytes do not match! !!!FAILED!!!");
                    fail("Decoded bytes do not match original bytes!");
                }

                bytes   = null;
                decoded = null;
            }

            print("| 1000 Xids encoded successfully");
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
            fail("Exception thrown during test: "+t.getMessage());
        }
        finally
        {
            print("|");
            print("|------------------------ END ------------------------");
        }
    }

    private boolean byteArraysEqual(byte[] A, byte[] B)
    {
        if (A.length != B.length)
        {
            return false;
        }

        for (int i=0; i<A.length; i++)
        {
            if (A[i] != B[i])
            {
                return false;
            }
        }

        return true;
    }
}
