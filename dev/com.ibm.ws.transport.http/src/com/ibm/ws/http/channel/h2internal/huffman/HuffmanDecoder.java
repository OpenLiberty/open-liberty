/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.huffman;

import java.util.Arrays;

import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class HuffmanDecoder {

    // Couple things to note:
    //   1. For performance reasons the compare methods are static in order to cut way down on class loading
    //   2. For performance reasons the non-static methods are not at all thread-safe, in fact they make ample use of class scoped variables rather than needlessly
    //      passing them around as local vars.
    //   3. So, the ugly brute-force compares are done instead of building and recursively traversing an elegant huffman tree.  This is because
    //      with only 256 value nodes, and with the smaller size huffman strings more likely to be hit, it is not at all certain to me that a recursive tree walking
    //      method is going to be any faster than just doing some compares in what will be a few methods most of the time, compares are super fast.  The
    //      worse case scenario is doing 255 compares, which is still pretty fast(er?) compare to recursively traversing a 30 level tree.
    //      So, keeping is simple, and maybe even faster, for now.

    private enum RESULT {
        NO,
        YES,
        EOS
    }

    private enum MORE_BITS {
        NO,
        YES
    }

    byte[] ba = null;
    int baSize = 0;
    byte[] results = null;

    int register32 = 0;
    int currentBitByteMask = 0; // mask for getting the next bit to look at
    byte nextByte = 0; // current byte being looked at
    int byteIndex = 0; // where we are in the Huffman encoded byte array
    int rIndex = 0; // index into the decoded ascii char array of where to put the next char.

    HuffmanChar huffmanChar = new HuffmanChar();

    public byte[] convertHuffmanToAscii(WsByteBuffer buffer, int length) throws Exception {

        // need to determine what to do with pos and limit setting after conversion, and if length needs to be an input value

        ba = new byte[length];
        buffer.get(ba);
        return convertHuffmanToAscii(ba);
    }

    public byte[] convertHuffmanToAscii(byte[] x1) throws CompressionException {
        // return length of converted ascii array
        rIndex = 0;

        ba = x1;
        baSize = ba.length;
        // Input: a byte array that holds the bytes to enocde
        // for example, b9 58 d3 3f, decodes to length 5,  value = :path

        // since huffman chars are at least 5 bits, new acsii byte array will be no more than twice as big
        results = new byte[baSize * 2];

        byteIndex = 0; // 0 based, where we are in the byte array
        nextByte = ba[byteIndex];
        rIndex = 0; // 0 based, index into the decoded ascii char array

        // get first 5 bits
        register32 = (nextByte >>> 3) & 0x1F; // right five bits in ba[0], now bottom five bits in register
        currentBitByteMask = 0x08; // last bit looked at from the left, so three more to look at.

        int checkNumber = 5;
        RESULT foundChar = RESULT.NO;
        MORE_BITS done = MORE_BITS.YES;

        while (true) {

            //System.out.println("checkNumber: " + checkNumber + " looking at: " + String.format("0x%08X", register32));
            foundChar = RESULT.NO;

            switch (checkNumber) {
                case 5:
                    foundChar = check5(register32, huffmanChar);
                    break;

                case 6:
                    foundChar = check6(register32, huffmanChar);
                    break;

                case 7:
                    foundChar = check7(register32, huffmanChar);
                    break;

                case 8:
                    foundChar = check8(register32, huffmanChar);
                    break;

                case 10:
                    foundChar = check10(register32, huffmanChar);
                    break;

                case 11:
                    foundChar = check11(register32, huffmanChar);
                    break;

                case 12:
                    foundChar = check12(register32, huffmanChar);
                    break;

                case 13:
                    foundChar = check13(register32, huffmanChar);
                    break;

                case 14:
                    foundChar = check14(register32, huffmanChar);
                    break;

                case 15:
                    foundChar = check15(register32, huffmanChar);
                    break;

                case 19:
                    foundChar = check19(register32, huffmanChar);
                    break;

                case 20:
                    foundChar = check20(register32, huffmanChar);
                    break;

                case 21:
                    foundChar = check21(register32, huffmanChar);
                    break;

                case 22:
                    foundChar = check22(register32, huffmanChar);
                    break;

                case 23:
                    foundChar = check23(register32, huffmanChar);
                    break;

                case 24:
                    foundChar = check24(register32, huffmanChar);
                    break;

                case 25:
                    foundChar = check25(register32, huffmanChar);
                    break;

                case 26:
                    foundChar = check26(register32, huffmanChar);
                    break;

                case 27:
                    foundChar = check27(register32, huffmanChar);
                    break;

                case 28:
                    foundChar = check28(register32, huffmanChar);
                    break;

                case 30:
                    foundChar = check30(register32, huffmanChar);
                    if (foundChar == RESULT.EOS) {
                        // System.out.println("found: EOS");
                        // found last char of EOS, which is not to be decoded into an ASCII char
                        return Arrays.copyOf(results, rIndex);
                    }
                    break;

                case 31:
                    // did not map the encoding
                    throw new CompressionException("Huffman hosed (4)");

                default:
                    // number without a check* will fall through, and moveToNextBit and update the counters

            }

            if (foundChar == RESULT.YES) {
                //	if ((huffmanChar.getAsciiValue()) > 0 && (huffmanChar.getAsciiValue() < 127)) {
                //		char c = (char) huffmanChar.getAsciiValue();
                //		System.out.println("found: " + huffmanChar.getAsciiValue() + " : " + c);
                //	} else {
                //		System.out.println("found: " + huffmanChar.getAsciiValue());
                //	}

                results[rIndex] = huffmanChar.getAsciiValue();
                rIndex++;

                // load up 5 more bits to start a new search
                register32 = 0;
                for (int i = 0; i < 5; i++) {
                    done = moveToNextBit();

                    // done if we found a char, and the next bit would be off the end,
                    //  Or the last char found was an EOS (which is tested for and handled above in "case 30")
                    if (done == MORE_BITS.NO) {
                        if (i == 0) {
                            return Arrays.copyOf(results, rIndex);
                        } else {
                            // if all current bits are 1's, then that is valid padding at the end
                            if (register32 == 1 || register32 == 3 || register32 == 7 || register32 == 15) {
                                return Arrays.copyOf(results, rIndex);
                            }

                            // invalid huffman sequence, or we mis-processed it
                            throw new CompressionException("Huffman hosed (1)");
                        }
                    }
                }

                checkNumber = 5;

            } else {
                // add in the next bit
                done = moveToNextBit();
                if (done == MORE_BITS.NO) {

                    // if up to the last 7 bits are all 1's, then that is valid padding
                    // we alraedy would have caught only 1-4 total bits of padding, so just check for 5 - 7 bits of valid padding
                    if (register32 == 31 || register32 == 63 || register32 == 127) {
                        return Arrays.copyOf(results, rIndex);
                    }

                    // invalid huffman sequence, or we mis-processed it
                    throw new CompressionException("Huffman hosed (2)");
                }

                checkNumber++;
            }
        }
    }

    public MORE_BITS moveToNextBit() {

        if (currentBitByteMask == 0x01) {
            byteIndex++;

            if (byteIndex >= baSize) {
                return MORE_BITS.NO;
            }

            nextByte = ba[byteIndex];
            currentBitByteMask = 0x80;

        } else {
            currentBitByteMask = currentBitByteMask >>> 1;
        }

        register32 = register32 << 1; // move one bit to the left, and use the logic below to determine the value of the new LSB

        if ((nextByte & currentBitByteMask) != 0) {
            // the bit we are looking at is set, so add it as set in the LSB in the new number we are decoded.
            // otherwise leave the new LSB as a 0
            register32++;
        }

        //System.out.println("moveToNextBit: nextByte: " +  String.format("0x%08X", nextByte)
        //           + " currentBitByteMask: "  + String.format("0x%08X", currentBitByteMask)
        //           + " register32 out: " + String.format("0x%8X", register32));

        return MORE_BITS.YES;
    }

    public static RESULT check5(int register, HuffmanChar hChar) {

        // '0' ( 48)  |00000                                         0  [ 5]
        // '1' ( 49)  |00001                                         1  [ 5]
        // '2' ( 50)  |00010                                         2  [ 5]
        // 'a' ( 97)  |00011                                         3  [ 5]
        // 'c' ( 99)  |00100                                         4  [ 5]
        // 'e' (101)  |00101                                         5  [ 5]
        // 'i' (105)  |00110                                         6  [ 5]
        // 'o' (111)  |00111                                         7  [ 5]
        // 's' (115)  |01000                                         8  [ 5]
        // 't' (116)  |01001                                         9  [ 5]

        if (register == 0x00000000) {
            hChar.setAsciiValue((byte) 48);
            return RESULT.YES;
        } else if (register == 0x00000001) {
            hChar.setAsciiValue((byte) 49);
            return RESULT.YES;
        } else if (register == 0x00000002) {
            hChar.setAsciiValue((byte) 50);
            return RESULT.YES;
        } else if (register == 0x00000003) {
            hChar.setAsciiValue((byte) 97);
            return RESULT.YES;
        } else if (register == 0x00000004) {
            hChar.setAsciiValue((byte) 99);
            return RESULT.YES;
        } else if (register == 0x00000005) {
            hChar.setAsciiValue((byte) 101);
            return RESULT.YES;
        } else if (register == 0x00000006) {
            hChar.setAsciiValue((byte) 105);
            return RESULT.YES;
        } else if (register == 0x00000007) {
            hChar.setAsciiValue((byte) 111);
            return RESULT.YES;
        } else if (register == 0x00000008) {
            hChar.setAsciiValue((byte) 115);
            return RESULT.YES;
        } else if (register == 0x00000009) {
            hChar.setAsciiValue((byte) 116);
            return RESULT.YES;
        }

        return RESULT.NO;

    }

    public static RESULT check6(int register, HuffmanChar hChar) {
        //		           ' ' ( 32)  |010100                                       14  [ 6]
        //				   '%' ( 37)  |010101                                       15  [ 6]
        //				   '-' ( 45)  |010110                                       16  [ 6]
        //				   '.' ( 46)  |010111                                       17  [ 6]
        //				   '/' ( 47)  |011000                                       18  [ 6]
        //				   '3' ( 51)  |011001                                       19  [ 6]
        //				   '4' ( 52)  |011010                                       1a  [ 6]
        //				   '5' ( 53)  |011011                                       1b  [ 6]
        //				   '6' ( 54)  |011100                                       1c  [ 6]
        //				   '7' ( 55)  |011101                                       1d  [ 6]
        //				   '8' ( 56)  |011110                                       1e  [ 6]
        //				   '9' ( 57)  |011111                                       1f  [ 6]
        //				   '=' ( 61)  |100000                                       20  [ 6]
        //				   'A' ( 65)  |100001                                       21  [ 6]
        //				   '_' ( 95)  |100010                                       22  [ 6]
        //				   'b' ( 98)  |100011                                       23  [ 6]
        //				   'd' (100)  |100100                                       24  [ 6]
        //				   'f' (102)  |100101                                       25  [ 6]
        //				   'g' (103)  |100110                                       26  [ 6]
        //				   'h' (104)  |100111                                       27  [ 6]
        //				   'l' (108)  |101000                                       28  [ 6]
        //				   'm' (109)  |101001                                       29  [ 6]
        //				   'n' (110)  |101010                                       2a  [ 6]
        //				   'p' (112)  |101011                                       2b  [ 6]
        //				   'r' (114)  |101100                                       2c  [ 6]
        //				   'u' (117)  |101101                                       2d  [ 6]

        if (register == 0x00000014) {
            hChar.setAsciiValue((byte) 32);
            return RESULT.YES;
        } else if (register == 0x00000015) {
            hChar.setAsciiValue((byte) 37);
            return RESULT.YES;
        } else if (register == 0x00000016) {
            hChar.setAsciiValue((byte) 45);
            return RESULT.YES;
        } else if (register == 0x00000017) {
            hChar.setAsciiValue((byte) 46);
            return RESULT.YES;
        } else if (register == 0x00000018) {
            hChar.setAsciiValue((byte) 47);
            return RESULT.YES;
        } else if (register == 0x00000019) {
            hChar.setAsciiValue((byte) 51);
            return RESULT.YES;
        } else if (register == 0x0000001a) {
            hChar.setAsciiValue((byte) 52);
            return RESULT.YES;
        } else if (register == 0x0000001b) {
            hChar.setAsciiValue((byte) 53);
            return RESULT.YES;
        } else if (register == 0x0000001c) {
            hChar.setAsciiValue((byte) 54);
            return RESULT.YES;
        } else if (register == 0x0000001d) {
            hChar.setAsciiValue((byte) 55);
            return RESULT.YES;
        } else if (register == 0x0000001e) {
            hChar.setAsciiValue((byte) 56);
            return RESULT.YES;
        } else if (register == 0x0000001f) {
            hChar.setAsciiValue((byte) 57);
            return RESULT.YES;
        } else if (register == 0x00000020) {
            hChar.setAsciiValue((byte) 61);
            return RESULT.YES;
        } else if (register == 0x00000021) {
            hChar.setAsciiValue((byte) 65);
            return RESULT.YES;
        } else if (register == 0x00000022) {
            hChar.setAsciiValue((byte) 95);
            return RESULT.YES;
        } else if (register == 0x00000023) {
            hChar.setAsciiValue((byte) 98);
            return RESULT.YES;
        } else if (register == 0x00000024) {
            hChar.setAsciiValue((byte) 100);
            return RESULT.YES;
        } else if (register == 0x00000025) {
            hChar.setAsciiValue((byte) 102);
            return RESULT.YES;
        } else if (register == 0x00000026) {
            hChar.setAsciiValue((byte) 103);
            return RESULT.YES;
        } else if (register == 0x00000027) {
            hChar.setAsciiValue((byte) 104);
            return RESULT.YES;
        } else if (register == 0x00000028) {
            hChar.setAsciiValue((byte) 108);
            return RESULT.YES;
        } else if (register == 0x00000029) {
            hChar.setAsciiValue((byte) 109);
            return RESULT.YES;
        } else if (register == 0x0000002a) {
            hChar.setAsciiValue((byte) 110);
            return RESULT.YES;
        } else if (register == 0x0000002b) {
            hChar.setAsciiValue((byte) 112);
            return RESULT.YES;
        } else if (register == 0x0000002c) {
            hChar.setAsciiValue((byte) 114);
            return RESULT.YES;
        } else if (register == 0x0000002d) {
            hChar.setAsciiValue((byte) 117);
            return RESULT.YES;
        }

        return RESULT.NO;

    }

    public static RESULT check7(int register, HuffmanChar hChar) {
        /*
         * ':' ( 58) |1011100 5c [ 7]
         * 'B' ( 66) |1011101 5d [ 7]
         * 'C' ( 67) |1011110 5e [ 7]
         * 'D' ( 68) |1011111 5f [ 7]
         * 'E' ( 69) |1100000 60 [ 7]
         * 'F' ( 70) |1100001 61 [ 7]
         * 'G' ( 71) |1100010 62 [ 7]
         * 'H' ( 72) |1100011 63 [ 7]
         * 'I' ( 73) |1100100 64 [ 7]
         * 'J' ( 74) |1100101 65 [ 7]
         * 'K' ( 75) |1100110 66 [ 7]
         * 'L' ( 76) |1100111 67 [ 7]
         * 'M' ( 77) |1101000 68 [ 7]
         * 'N' ( 78) |1101001 69 [ 7]
         * 'O' ( 79) |1101010 6a [ 7]
         * 'P' ( 80) |1101011 6b [ 7]
         * 'Q' ( 81) |1101100 6c [ 7]
         * 'R' ( 82) |1101101 6d [ 7]
         * 'S' ( 83) |1101110 6e [ 7]
         * 'T' ( 84) |1101111 6f [ 7]
         * 'U' ( 85) |1110000 70 [ 7]
         * 'V' ( 86) |1110001 71 [ 7]
         * 'W' ( 87) |1110010 72 [ 7]
         * 'Y' ( 89) |1110011 73 [ 7]
         * 'j' (106) |1110100 74 [ 7]
         * 'k' (107) |1110101 75 [ 7]
         * 'q' (113) |1110110 76 [ 7]
         * 'v' (118) |1110111 77 [ 7]
         * 'w' (119) |1111000 78 [ 7]
         * 'x' (120) |1111001 79 [ 7]
         * 'y' (121) |1111010 7a [ 7]
         * 'z' (122) |1111011 7b [ 7]
         */

        if (register == 0x0000005c) {
            hChar.setAsciiValue((byte) 58);
            return RESULT.YES;
        } else if (register == 0x0000005d) {
            hChar.setAsciiValue((byte) 66);
            return RESULT.YES;
        } else if (register == 0x0000005e) {
            hChar.setAsciiValue((byte) 67);
            return RESULT.YES;
        } else if (register == 0x0000005f) {
            hChar.setAsciiValue((byte) 68);
            return RESULT.YES;
        } else if (register == 0x00000060) {
            hChar.setAsciiValue((byte) 69);
            return RESULT.YES;
        } else if (register == 0x00000061) {
            hChar.setAsciiValue((byte) 70);
            return RESULT.YES;
        } else if (register == 0x00000062) {
            hChar.setAsciiValue((byte) 71);
            return RESULT.YES;
        } else if (register == 0x00000063) {
            hChar.setAsciiValue((byte) 72);
            return RESULT.YES;
        } else if (register == 0x00000064) {
            hChar.setAsciiValue((byte) 73);
            return RESULT.YES;
        } else if (register == 0x00000065) {
            hChar.setAsciiValue((byte) 74);
            return RESULT.YES;
        } else if (register == 0x00000066) {
            hChar.setAsciiValue((byte) 75);
            return RESULT.YES;
        } else if (register == 0x00000067) {
            hChar.setAsciiValue((byte) 76);
            return RESULT.YES;
        } else if (register == 0x00000068) {
            hChar.setAsciiValue((byte) 77);
            return RESULT.YES;
        } else if (register == 0x00000069) {
            hChar.setAsciiValue((byte) 78);
            return RESULT.YES;
        } else if (register == 0x0000006a) {
            hChar.setAsciiValue((byte) 79);
            return RESULT.YES;
        } else if (register == 0x0000006b) {
            hChar.setAsciiValue((byte) 80);
            return RESULT.YES;
        } else if (register == 0x0000006c) {
            hChar.setAsciiValue((byte) 81);
            return RESULT.YES;
        } else if (register == 0x0000006d) {
            hChar.setAsciiValue((byte) 82);
            return RESULT.YES;
        } else if (register == 0x0000006e) {
            hChar.setAsciiValue((byte) 83);
            return RESULT.YES;
        } else if (register == 0x0000006f) {
            hChar.setAsciiValue((byte) 84);
            return RESULT.YES;
        } else if (register == 0x00000070) {
            hChar.setAsciiValue((byte) 85);
            return RESULT.YES;
        } else if (register == 0x00000071) {
            hChar.setAsciiValue((byte) 86);
            return RESULT.YES;
        } else if (register == 0x00000072) {
            hChar.setAsciiValue((byte) 87);
            return RESULT.YES;
        } else if (register == 0x00000073) {
            hChar.setAsciiValue((byte) 89);
            return RESULT.YES;
        } else if (register == 0x00000074) {
            hChar.setAsciiValue((byte) 106);
            return RESULT.YES;
        } else if (register == 0x00000075) {
            hChar.setAsciiValue((byte) 107);
            return RESULT.YES;
        } else if (register == 0x00000076) {
            hChar.setAsciiValue((byte) 113);
            return RESULT.YES;
        } else if (register == 0x00000077) {
            hChar.setAsciiValue((byte) 118);
            return RESULT.YES;
        } else if (register == 0x00000078) {
            hChar.setAsciiValue((byte) 119);
            return RESULT.YES;
        } else if (register == 0x00000079) {
            hChar.setAsciiValue((byte) 120);
            return RESULT.YES;
        } else if (register == 0x0000007a) {
            hChar.setAsciiValue((byte) 121);
            return RESULT.YES;
        } else if (register == 0x0000007b) {
            hChar.setAsciiValue((byte) 122);
            return RESULT.YES;
        }

        return RESULT.NO;

    }

    public static RESULT check8(int register, HuffmanChar hChar) {
        /*
         * '&' ( 38) |11111000 f8 [ 8]
         * '*' ( 42) |11111001 f9 [ 8]
         * ',' ( 44) |11111010 fa [ 8]
         * ';' ( 59) |11111011 fb [ 8]
         * 'X' ( 88) |11111100 fc [ 8]
         * 'Z' ( 90) |11111101 fd [ 8]
         */

        if (register == 0x000000f8) {
            hChar.setAsciiValue((byte) 38);
            return RESULT.YES;
        } else if (register == 0x000000f9) {
            hChar.setAsciiValue((byte) 42);
            return RESULT.YES;
        } else if (register == 0x000000fa) {
            hChar.setAsciiValue((byte) 44);
            return RESULT.YES;
        } else if (register == 0x000000fb) {
            hChar.setAsciiValue((byte) 59);
            return RESULT.YES;
        } else if (register == 0x000000fc) {
            hChar.setAsciiValue((byte) 88);
            return RESULT.YES;
        } else if (register == 0x000000fd) {
            hChar.setAsciiValue((byte) 90);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check10(int register, HuffmanChar hChar) {

        /*
         * '!' ( 33) |11111110|00 3f8 [10]
         * '"' ( 34) |11111110|01 3f9 [10]
         * '(' ( 40) |11111110|10 3fa [10]
         * ')' ( 41) |11111110|11 3fb [10]
         * '?' ( 63) |11111111|00 3fc [10]
         */

        if (register == 0x000003f8) {
            hChar.setAsciiValue((byte) 33);
            return RESULT.YES;
        } else if (register == 0x000003f9) {
            hChar.setAsciiValue((byte) 34);
            return RESULT.YES;
        } else if (register == 0x000003fa) {
            hChar.setAsciiValue((byte) 40);
            return RESULT.YES;
        } else if (register == 0x000003fb) {
            hChar.setAsciiValue((byte) 41);
            return RESULT.YES;
        } else if (register == 0x000003fc) {
            hChar.setAsciiValue((byte) 63);
            return RESULT.YES;
        }

        return RESULT.NO;

    }

    public static RESULT check11(int register, HuffmanChar hChar) {
        /*
         * ''' ( 39) |11111111|010 7fa [11]
         * '+' ( 43) |11111111|011 7fb [11]
         * '|' (124) |11111111|100 7fc [11]
         */

        if (register == 0x000007fa) {
            hChar.setAsciiValue((byte) 39);
            return RESULT.YES;
        } else if (register == 0x000007fb) {
            hChar.setAsciiValue((byte) 43);
            return RESULT.YES;
        } else if (register == 0x000007fc) {
            hChar.setAsciiValue((byte) 124);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check12(int register, HuffmanChar hChar) {
        /*
         * '#' ( 35) |11111111|1010 ffa [12]
         * '>' ( 62) |11111111|1011 ffb [12]
         */

        if (register == 0x00000ffa) {
            hChar.setAsciiValue((byte) 35);
            return RESULT.YES;
        } else if (register == 0x00000ffb) {
            hChar.setAsciiValue((byte) 62);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check13(int register, HuffmanChar hChar) {

        /*
         * ( 0) |11111111|11000 1ff8 [13]
         * '$' ( 36) |11111111|11001 1ff9 [13]
         * '@' ( 64) |11111111|11010 1ffa [13]
         * '[' ( 91) |11111111|11011 1ffb [13]
         * ']' ( 93) |11111111|11100 1ffc [13]
         * '~' (126) |11111111|11101 1ffd [13]
         */

        if (register == 0x00001ff8) {
            hChar.setAsciiValue((byte) 0);
            return RESULT.YES;
        } else if (register == 0x00001ff9) {
            hChar.setAsciiValue((byte) 36);
            return RESULT.YES;
        } else if (register == 0x00001ffa) {
            hChar.setAsciiValue((byte) 64);
            return RESULT.YES;
        } else if (register == 0x00001ffb) {
            hChar.setAsciiValue((byte) 91);
            return RESULT.YES;
        } else if (register == 0x00001ffc) {
            hChar.setAsciiValue((byte) 93);
            return RESULT.YES;
        } else if (register == 0x00001ffd) {
            hChar.setAsciiValue((byte) 126);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check14(int register, HuffmanChar hChar) {
        /*
         * '^' ( 94) |11111111|111100 3ffc [14]
         * '}' (125) |11111111|111101 3ffd [14]
         */

        if (register == 0x00003ffc) {
            hChar.setAsciiValue((byte) 94);
            return RESULT.YES;
        } else if (register == 0x00003ffd) {
            hChar.setAsciiValue((byte) 125);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check15(int register, HuffmanChar hChar) {
        /*
         * '<' ( 60) |11111111|1111100 7ffc [15]
         * '`' ( 96) |11111111|1111101 7ffd [15]
         * '{' (123) |11111111|1111110 7ffe [15]
         */

        if (register == 0x00007ffc) {
            hChar.setAsciiValue((byte) 60);
            return RESULT.YES;
        } else if (register == 0x00007ffd) {
            hChar.setAsciiValue((byte) 96);
            return RESULT.YES;
        } else if (register == 0x00007ffe) {
            hChar.setAsciiValue((byte) 123);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check19(int register, HuffmanChar hChar) {
        /*
         * '\' ( 92) |11111111|11111110|000 7fff0 [19]
         * (195) |11111111|11111110|001 7fff1 [19]
         * (208) |11111111|11111110|010 7fff2 [19]
         */

        if (register == 0x0007fff0) {
            hChar.setAsciiValue((byte) 92);
            return RESULT.YES;
        } else if (register == 0x0007fff1) {
            hChar.setAsciiValue((byte) 195);
            return RESULT.YES;
        } else if (register == 0x0007fff2) {
            hChar.setAsciiValue((byte) 208);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check20(int register, HuffmanChar hChar) {
        /*
         * (128) |11111111|11111110|0110 fffe6 [20]
         * (130) |11111111|11111110|0111 fffe7 [20]
         * (131) |11111111|11111110|1000 fffe8 [20]
         * (162) |11111111|11111110|1001 fffe9 [20]
         * (184) |11111111|11111110|1010 fffea [20]
         * (194) |11111111|11111110|1011 fffeb [20]
         * (224) |11111111|11111110|1100 fffec [20]
         * (226) |11111111|11111110|1101 fffed [20]
         */

        if (register == 0x000fffe6) {
            hChar.setAsciiValue((byte) 128);
            return RESULT.YES;
        } else if (register == 0x000fffe7) {
            hChar.setAsciiValue((byte) 130);
            return RESULT.YES;
        } else if (register == 0x000fffe8) {
            hChar.setAsciiValue((byte) 131);
            return RESULT.YES;
        } else if (register == 0x000fffe9) {
            hChar.setAsciiValue((byte) 162);
            return RESULT.YES;
        } else if (register == 0x000fffea) {
            hChar.setAsciiValue((byte) 184);
            return RESULT.YES;
        } else if (register == 0x000fffeb) {
            hChar.setAsciiValue((byte) 194);
            return RESULT.YES;
        } else if (register == 0x000fffec) {
            hChar.setAsciiValue((byte) 224);
            return RESULT.YES;
        } else if (register == 0x000fffed) {
            hChar.setAsciiValue((byte) 226);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check21(int register, HuffmanChar hChar) {
        /*
         * (153) |11111111|11111110|11100 1fffdc [21]
         * (161) |11111111|11111110|11101 1fffdd [21]
         * (167) |11111111|11111110|11110 1fffde [21]
         * (172) |11111111|11111110|11111 1fffdf [21]
         * (176) |11111111|11111111|00000 1fffe0 [21]
         * (177) |11111111|11111111|00001 1fffe1 [21]
         * (179) |11111111|11111111|00010 1fffe2 [21]
         * (209) |11111111|11111111|00011 1fffe3 [21]
         * (216) |11111111|11111111|00100 1fffe4 [21]
         * (217) |11111111|11111111|00101 1fffe5 [21]
         * (227) |11111111|11111111|00110 1fffe6 [21]
         * (229) |11111111|11111111|00111 1fffe7 [21]
         * (230) |11111111|11111111|01000 1fffe8 [21]
         */

        if (register == 0x001fffdc) {
            hChar.setAsciiValue((byte) 153);
            return RESULT.YES;
        } else if (register == 0x001fffdd) {
            hChar.setAsciiValue((byte) 161);
            return RESULT.YES;
        } else if (register == 0x001fffde) {
            hChar.setAsciiValue((byte) 167);
            return RESULT.YES;
        } else if (register == 0x001fffdf) {
            hChar.setAsciiValue((byte) 172);
            return RESULT.YES;
        } else if (register == 0x001fffe0) {
            hChar.setAsciiValue((byte) 176);
            return RESULT.YES;
        } else if (register == 0x001fffe1) {
            hChar.setAsciiValue((byte) 177);
            return RESULT.YES;
        } else if (register == 0x001fffe2) {
            hChar.setAsciiValue((byte) 179);
            return RESULT.YES;
        } else if (register == 0x001fffe3) {
            hChar.setAsciiValue((byte) 209);
            return RESULT.YES;
        } else if (register == 0x001fffe4) {
            hChar.setAsciiValue((byte) 216);
            return RESULT.YES;
        } else if (register == 0x001fffe5) {
            hChar.setAsciiValue((byte) 217);
            return RESULT.YES;
        } else if (register == 0x001fffe6) {
            hChar.setAsciiValue((byte) 227);
            return RESULT.YES;
        } else if (register == 0x001fffe7) {
            hChar.setAsciiValue((byte) 229);
            return RESULT.YES;
        } else if (register == 0x001fffe8) {
            hChar.setAsciiValue((byte) 230);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check22(int register, HuffmanChar hChar) {
        /*
         * (129) |11111111|11111111|010010 3fffd2 [22]
         * (132) |11111111|11111111|010011 3fffd3 [22]
         * (133) |11111111|11111111|010100 3fffd4 [22]
         * (134) |11111111|11111111|010101 3fffd5 [22]
         * (136) |11111111|11111111|010110 3fffd6 [22]
         * (146) |11111111|11111111|010111 3fffd7 [22]
         * (154) |11111111|11111111|011000 3fffd8 [22]
         * (156) |11111111|11111111|011001 3fffd9 [22]
         * (160) |11111111|11111111|011010 3fffda [22]
         * (163) |11111111|11111111|011011 3fffdb [22]
         * (164) |11111111|11111111|011100 3fffdc [22]
         * (169) |11111111|11111111|011101 3fffdd [22]
         * (170) |11111111|11111111|011110 3fffde [22]
         * (173) |11111111|11111111|011111 3fffdf [22]
         * (178) |11111111|11111111|100000 3fffe0 [22]
         * (181) |11111111|11111111|100001 3fffe1 [22]
         * (185) |11111111|11111111|100010 3fffe2 [22]
         * (186) |11111111|11111111|100011 3fffe3 [22]
         * (187) |11111111|11111111|100100 3fffe4 [22]
         * (189) |11111111|11111111|100101 3fffe5 [22]
         * (190) |11111111|11111111|100110 3fffe6 [22]
         * (196) |11111111|11111111|100111 3fffe7 [22]
         * (198) |11111111|11111111|101000 3fffe8 [22]
         * (228) |11111111|11111111|101001 3fffe9 [22]
         * (232) |11111111|11111111|101010 3fffea [22]
         * (233) |11111111|11111111|101011 3fffeb [22]
         */

        if (register == 0x003fffd2) {
            hChar.setAsciiValue((byte) 129);
            return RESULT.YES;
        } else if (register == 0x003fffd3) {
            hChar.setAsciiValue((byte) 132);
            return RESULT.YES;
        } else if (register == 0x003fffd4) {
            hChar.setAsciiValue((byte) 133);
            return RESULT.YES;
        } else if (register == 0x003fffd5) {
            hChar.setAsciiValue((byte) 134);
            return RESULT.YES;
        } else if (register == 0x003fffd6) {
            hChar.setAsciiValue((byte) 136);
            return RESULT.YES;
        } else if (register == 0x003fffd7) {
            hChar.setAsciiValue((byte) 146);
            return RESULT.YES;
        } else if (register == 0x003fffd8) {
            hChar.setAsciiValue((byte) 154);
            return RESULT.YES;
        } else if (register == 0x003fffd9) {
            hChar.setAsciiValue((byte) 156);
            return RESULT.YES;
        } else if (register == 0x003fffda) {
            hChar.setAsciiValue((byte) 160);
            return RESULT.YES;
        } else if (register == 0x003fffdb) {
            hChar.setAsciiValue((byte) 163);
            return RESULT.YES;
        } else if (register == 0x003fffdc) {
            hChar.setAsciiValue((byte) 164);
            return RESULT.YES;
        } else if (register == 0x003fffdd) {
            hChar.setAsciiValue((byte) 169);
            return RESULT.YES;
        } else if (register == 0x003fffde) {
            hChar.setAsciiValue((byte) 170);
            return RESULT.YES;
        } else if (register == 0x003fffdf) {
            hChar.setAsciiValue((byte) 173);
            return RESULT.YES;
        } else if (register == 0x003fffe0) {
            hChar.setAsciiValue((byte) 178);
            return RESULT.YES;
        } else if (register == 0x003fffe1) {
            hChar.setAsciiValue((byte) 181);
            return RESULT.YES;
        } else if (register == 0x003fffe2) {
            hChar.setAsciiValue((byte) 185);
            return RESULT.YES;
        } else if (register == 0x003fffe3) {
            hChar.setAsciiValue((byte) 186);
            return RESULT.YES;
        } else if (register == 0x003fffe4) {
            hChar.setAsciiValue((byte) 187);
            return RESULT.YES;
        } else if (register == 0x003fffe5) {
            hChar.setAsciiValue((byte) 189);
            return RESULT.YES;
        } else if (register == 0x003fffe6) {
            hChar.setAsciiValue((byte) 190);
            return RESULT.YES;
        } else if (register == 0x003fffe7) {
            hChar.setAsciiValue((byte) 196);
            return RESULT.YES;
        } else if (register == 0x003fffe8) {
            hChar.setAsciiValue((byte) 198);
            return RESULT.YES;
        } else if (register == 0x003fffe9) {
            hChar.setAsciiValue((byte) 228);
            return RESULT.YES;
        } else if (register == 0x003fffea) {
            hChar.setAsciiValue((byte) 232);
            return RESULT.YES;
        } else if (register == 0x003fffeb) {
            hChar.setAsciiValue((byte) 233);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check23(int register, HuffmanChar hChar) {
        /*
         * ( 1) |11111111|11111111|1011000 7fffd8 [23]
         * (135) |11111111|11111111|1011001 7fffd9 [23]
         * (137) |11111111|11111111|1011010 7fffda [23]
         * (138) |11111111|11111111|1011011 7fffdb [23]
         * (139) |11111111|11111111|1011100 7fffdc [23]
         * (140) |11111111|11111111|1011101 7fffdd [23]
         * (141) |11111111|11111111|1011110 7fffde [23]
         * (143) |11111111|11111111|1011111 7fffdf [23]
         * (147) |11111111|11111111|1100000 7fffe0 [23]
         * (149) |11111111|11111111|1100001 7fffe1 [23]
         * (150) |11111111|11111111|1100010 7fffe2 [23]
         * (151) |11111111|11111111|1100011 7fffe3 [23]
         * (152) |11111111|11111111|1100100 7fffe4 [23]
         * (155) |11111111|11111111|1100101 7fffe5 [23]
         * (157) |11111111|11111111|1100110 7fffe6 [23]
         * (158) |11111111|11111111|1100111 7fffe7 [23]
         * (165) |11111111|11111111|1101000 7fffe8 [23]
         * (166) |11111111|11111111|1101001 7fffe9 [23]
         * (168) |11111111|11111111|1101010 7fffea [23]
         * (174) |11111111|11111111|1101011 7fffeb [23]
         * (175) |11111111|11111111|1101100 7fffec [23]
         * (180) |11111111|11111111|1101101 7fffed [23]
         * (182) |11111111|11111111|1101110 7fffee [23]
         * (183) |11111111|11111111|1101111 7fffef [23]
         * (188) |11111111|11111111|1110000 7ffff0 [23]
         * (191) |11111111|11111111|1110001 7ffff1 [23]
         * (197) |11111111|11111111|1110010 7ffff2 [23]
         * (231) |11111111|11111111|1110011 7ffff3 [23]
         * (239) |11111111|11111111|1110100 7ffff4 [23]
         */

        if (register == 0x007fffd8) {
            hChar.setAsciiValue((byte) 1);
            return RESULT.YES;
        } else if (register == 0x007fffd9) {
            hChar.setAsciiValue((byte) 135);
            return RESULT.YES;
        } else if (register == 0x007fffda) {
            hChar.setAsciiValue((byte) 137);
            return RESULT.YES;
        } else if (register == 0x007fffdb) {
            hChar.setAsciiValue((byte) 138);
            return RESULT.YES;
        } else if (register == 0x007fffdc) {
            hChar.setAsciiValue((byte) 139);
            return RESULT.YES;
        } else if (register == 0x007fffdd) {
            hChar.setAsciiValue((byte) 140);
            return RESULT.YES;
        } else if (register == 0x007fffde) {
            hChar.setAsciiValue((byte) 141);
            return RESULT.YES;
        } else if (register == 0x007fffdf) {
            hChar.setAsciiValue((byte) 143);
            return RESULT.YES;
        } else if (register == 0x007fffe0) {
            hChar.setAsciiValue((byte) 147);
            return RESULT.YES;
        } else if (register == 0x007fffe1) {
            hChar.setAsciiValue((byte) 149);
            return RESULT.YES;
        } else if (register == 0x007fffe2) {
            hChar.setAsciiValue((byte) 150);
            return RESULT.YES;
        } else if (register == 0x007fffe3) {
            hChar.setAsciiValue((byte) 151);
            return RESULT.YES;
        } else if (register == 0x007fffe4) {
            hChar.setAsciiValue((byte) 152);
            return RESULT.YES;
        } else if (register == 0x007fffe5) {
            hChar.setAsciiValue((byte) 155);
            return RESULT.YES;
        } else if (register == 0x007fffe6) {
            hChar.setAsciiValue((byte) 157);
            return RESULT.YES;
        } else if (register == 0x007fffe7) {
            hChar.setAsciiValue((byte) 158);
            return RESULT.YES;
        } else if (register == 0x007fffe8) {
            hChar.setAsciiValue((byte) 165);
            return RESULT.YES;
        } else if (register == 0x007fffe9) {
            hChar.setAsciiValue((byte) 166);
            return RESULT.YES;
        } else if (register == 0x007fffea) {
            hChar.setAsciiValue((byte) 168);
            return RESULT.YES;
        } else if (register == 0x007fffeb) {
            hChar.setAsciiValue((byte) 174);
            return RESULT.YES;
        } else if (register == 0x007fffec) {
            hChar.setAsciiValue((byte) 175);
            return RESULT.YES;
        } else if (register == 0x007fffed) {
            hChar.setAsciiValue((byte) 180);
            return RESULT.YES;
        } else if (register == 0x007fffee) {
            hChar.setAsciiValue((byte) 182);
            return RESULT.YES;
        } else if (register == 0x007fffef) {
            hChar.setAsciiValue((byte) 183);
            return RESULT.YES;
        } else if (register == 0x007ffff0) {
            hChar.setAsciiValue((byte) 188);
            return RESULT.YES;
        } else if (register == 0x007ffff1) {
            hChar.setAsciiValue((byte) 191);
            return RESULT.YES;
        } else if (register == 0x007ffff2) {
            hChar.setAsciiValue((byte) 197);
            return RESULT.YES;
        } else if (register == 0x007ffff3) {
            hChar.setAsciiValue((byte) 231);
            return RESULT.YES;
        } else if (register == 0x007ffff4) {
            hChar.setAsciiValue((byte) 239);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check24(int register, HuffmanChar hChar) {
        /*
         * ( 9) |11111111|11111111|11101010 ffffea [24]
         * (142) |11111111|11111111|11101011 ffffeb [24]
         * (144) |11111111|11111111|11101100 ffffec [24]
         * (145) |11111111|11111111|11101101 ffffed [24]
         * (148) |11111111|11111111|11101110 ffffee [24]
         * (159) |11111111|11111111|11101111 ffffef [24]
         * (171) |11111111|11111111|11110000 fffff0 [24]
         * (206) |11111111|11111111|11110001 fffff1 [24]
         * (215) |11111111|11111111|11110010 fffff2 [24]
         * (225) |11111111|11111111|11110011 fffff3 [24]
         * (236) |11111111|11111111|11110100 fffff4 [24]
         * (237) |11111111|11111111|11110101 fffff5 [24]
         */

        if (register == 0x00ffffea) {
            hChar.setAsciiValue((byte) 9);
            return RESULT.YES;
        } else if (register == 0x00ffffeb) {
            hChar.setAsciiValue((byte) 142);
            return RESULT.YES;
        } else if (register == 0x00ffffec) {
            hChar.setAsciiValue((byte) 144);
            return RESULT.YES;
        } else if (register == 0x00ffffed) {
            hChar.setAsciiValue((byte) 145);
            return RESULT.YES;
        } else if (register == 0x00ffffee) {
            hChar.setAsciiValue((byte) 148);
            return RESULT.YES;
        } else if (register == 0x00ffffef) {
            hChar.setAsciiValue((byte) 159);
            return RESULT.YES;
        } else if (register == 0x00fffff0) {
            hChar.setAsciiValue((byte) 171);
            return RESULT.YES;
        } else if (register == 0x00fffff1) {
            hChar.setAsciiValue((byte) 206);
            return RESULT.YES;
        } else if (register == 0x00fffff2) {
            hChar.setAsciiValue((byte) 215);
            return RESULT.YES;
        } else if (register == 0x00fffff3) {
            hChar.setAsciiValue((byte) 225);
            return RESULT.YES;
        } else if (register == 0x00fffff4) {
            hChar.setAsciiValue((byte) 236);
            return RESULT.YES;
        } else if (register == 0x00fffff5) {
            hChar.setAsciiValue((byte) 237);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check25(int register, HuffmanChar hChar) {
        /*
         * (199) |11111111|11111111|11110110|0 1ffffec [25]
         * (207) |11111111|11111111|11110110|1 1ffffed [25]
         * (234) |11111111|11111111|11110111|0 1ffffee [25]
         * (235) |11111111|11111111|11110111|1 1ffffef [25]
         */

        if (register == 0x01ffffec) {
            hChar.setAsciiValue((byte) 199);
            return RESULT.YES;
        } else if (register == 0x01ffffed) {
            hChar.setAsciiValue((byte) 207);
            return RESULT.YES;
        } else if (register == 0x01ffffee) {
            hChar.setAsciiValue((byte) 234);
            return RESULT.YES;
        } else if (register == 0x01ffffef) {
            hChar.setAsciiValue((byte) 235);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check26(int register, HuffmanChar hChar) {
        /*
         * (192) |11111111|11111111|11111000|00 3ffffe0 [26]
         * (193) |11111111|11111111|11111000|01 3ffffe1 [26]
         * (200) |11111111|11111111|11111000|10 3ffffe2 [26]
         * (201) |11111111|11111111|11111000|11 3ffffe3 [26]
         * (202) |11111111|11111111|11111001|00 3ffffe4 [26]
         * (205) |11111111|11111111|11111001|01 3ffffe5 [26]
         * (210) |11111111|11111111|11111001|10 3ffffe6 [26]
         * (213) |11111111|11111111|11111001|11 3ffffe7 [26]
         * (218) |11111111|11111111|11111010|00 3ffffe8 [26]
         * (219) |11111111|11111111|11111010|01 3ffffe9 [26]
         * (238) |11111111|11111111|11111010|10 3ffffea [26]
         * (240) |11111111|11111111|11111010|11 3ffffeb [26]
         * (242) |11111111|11111111|11111011|00 3ffffec [26]
         * (243) |11111111|11111111|11111011|01 3ffffed [26]
         * (255) |11111111|11111111|11111011|10 3ffffee [26]
         */

        if (register == 0x03ffffe0) {
            hChar.setAsciiValue((byte) 192);
            return RESULT.YES;
        } else if (register == 0x03ffffe1) {
            hChar.setAsciiValue((byte) 193);
            return RESULT.YES;
        } else if (register == 0x03ffffe2) {
            hChar.setAsciiValue((byte) 200);
            return RESULT.YES;
        } else if (register == 0x03ffffe3) {
            hChar.setAsciiValue((byte) 201);
            return RESULT.YES;
        } else if (register == 0x03ffffe4) {
            hChar.setAsciiValue((byte) 202);
            return RESULT.YES;
        } else if (register == 0x03ffffe5) {
            hChar.setAsciiValue((byte) 205);
            return RESULT.YES;
        } else if (register == 0x03ffffe6) {
            hChar.setAsciiValue((byte) 210);
            return RESULT.YES;
        } else if (register == 0x03ffffe7) {
            hChar.setAsciiValue((byte) 213);
            return RESULT.YES;
        } else if (register == 0x03ffffe8) {
            hChar.setAsciiValue((byte) 218);
            return RESULT.YES;
        } else if (register == 0x03ffffe9) {
            hChar.setAsciiValue((byte) 219);
            return RESULT.YES;
        } else if (register == 0x03ffffea) {
            hChar.setAsciiValue((byte) 238);
            return RESULT.YES;
        } else if (register == 0x03ffffeb) {
            hChar.setAsciiValue((byte) 240);
            return RESULT.YES;
        } else if (register == 0x03ffffec) {
            hChar.setAsciiValue((byte) 242);
            return RESULT.YES;
        } else if (register == 0x03ffffed) {
            hChar.setAsciiValue((byte) 243);
            return RESULT.YES;
        } else if (register == 0x03ffffee) {
            hChar.setAsciiValue((byte) 255);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check27(int register, HuffmanChar hChar) {
        /*
         * (203) |11111111|11111111|11111011|110 7ffffde [27]
         * (204) |11111111|11111111|11111011|111 7ffffdf [27]
         * (211) |11111111|11111111|11111100|000 7ffffe0 [27]
         * (212) |11111111|11111111|11111100|001 7ffffe1 [27]
         * (214) |11111111|11111111|11111100|010 7ffffe2 [27]
         * (221) |11111111|11111111|11111100|011 7ffffe3 [27]
         * (222) |11111111|11111111|11111100|100 7ffffe4 [27]
         * (223) |11111111|11111111|11111100|101 7ffffe5 [27]
         * (241) |11111111|11111111|11111100|110 7ffffe6 [27]
         * (244) |11111111|11111111|11111100|111 7ffffe7 [27]
         * (245) |11111111|11111111|11111101|000 7ffffe8 [27]
         * (246) |11111111|11111111|11111101|001 7ffffe9 [27]
         * (247) |11111111|11111111|11111101|010 7ffffea [27]
         * (248) |11111111|11111111|11111101|011 7ffffeb [27]
         * (250) |11111111|11111111|11111101|100 7ffffec [27]
         * (251) |11111111|11111111|11111101|101 7ffffed [27]
         * (252) |11111111|11111111|11111101|110 7ffffee [27]
         * (253) |11111111|11111111|11111101|111 7ffffef [27]
         * (254) |11111111|11111111|11111110|000 7fffff0 [27]
         */

        if (register == 0x07ffffde) {
            hChar.setAsciiValue((byte) 203);
            return RESULT.YES;
        } else if (register == 0x07ffffdf) {
            hChar.setAsciiValue((byte) 204);
            return RESULT.YES;
        } else if (register == 0x07ffffe0) {
            hChar.setAsciiValue((byte) 211);
            return RESULT.YES;
        } else if (register == 0x07ffffe1) {
            hChar.setAsciiValue((byte) 212);
            return RESULT.YES;
        } else if (register == 0x07ffffe2) {
            hChar.setAsciiValue((byte) 214);
            return RESULT.YES;
        } else if (register == 0x07ffffe3) {
            hChar.setAsciiValue((byte) 221);
            return RESULT.YES;
        } else if (register == 0x07ffffe4) {
            hChar.setAsciiValue((byte) 222);
            return RESULT.YES;
        } else if (register == 0x07ffffe5) {
            hChar.setAsciiValue((byte) 223);
            return RESULT.YES;
        } else if (register == 0x07ffffe6) {
            hChar.setAsciiValue((byte) 241);
            return RESULT.YES;
        } else if (register == 0x07ffffe7) {
            hChar.setAsciiValue((byte) 244);
            return RESULT.YES;
        } else if (register == 0x07ffffe8) {
            hChar.setAsciiValue((byte) 245);
            return RESULT.YES;
        } else if (register == 0x07ffffe9) {
            hChar.setAsciiValue((byte) 246);
            return RESULT.YES;
        } else if (register == 0x07ffffea) {
            hChar.setAsciiValue((byte) 247);
            return RESULT.YES;
        } else if (register == 0x07ffffeb) {
            hChar.setAsciiValue((byte) 248);
            return RESULT.YES;
        } else if (register == 0x07ffffec) {
            hChar.setAsciiValue((byte) 250);
            return RESULT.YES;
        } else if (register == 0x07ffffed) {
            hChar.setAsciiValue((byte) 251);
            return RESULT.YES;
        } else if (register == 0x07ffffee) {
            hChar.setAsciiValue((byte) 252);
            return RESULT.YES;
        } else if (register == 0x07ffffef) {
            hChar.setAsciiValue((byte) 253);
            return RESULT.YES;
        } else if (register == 0x07fffff0) {
            hChar.setAsciiValue((byte) 254);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check28(int register, HuffmanChar hChar) {
        /*
         * ( 2) |11111111|11111111|11111110|0010 fffffe2 [28]
         * ( 3) |11111111|11111111|11111110|0011 fffffe3 [28]
         * ( 4) |11111111|11111111|11111110|0100 fffffe4 [28]
         * ( 5) |11111111|11111111|11111110|0101 fffffe5 [28]
         * ( 6) |11111111|11111111|11111110|0110 fffffe6 [28]
         * ( 7) |11111111|11111111|11111110|0111 fffffe7 [28]
         * ( 8) |11111111|11111111|11111110|1000 fffffe8 [28]
         * ( 11) |11111111|11111111|11111110|1001 fffffe9 [28]
         * ( 12) |11111111|11111111|11111110|1010 fffffea [28]
         * ( 14) |11111111|11111111|11111110|1011 fffffeb [28]
         * ( 15) |11111111|11111111|11111110|1100 fffffec [28]
         * ( 16) |11111111|11111111|11111110|1101 fffffed [28]
         * ( 17) |11111111|11111111|11111110|1110 fffffee [28]
         * ( 18) |11111111|11111111|11111110|1111 fffffef [28]
         * ( 19) |11111111|11111111|11111111|0000 ffffff0 [28]
         * ( 20) |11111111|11111111|11111111|0001 ffffff1 [28]
         * ( 21) |11111111|11111111|11111111|0010 ffffff2 [28]
         * ( 23) |11111111|11111111|11111111|0011 ffffff3 [28]
         * ( 24) |11111111|11111111|11111111|0100 ffffff4 [28]
         * ( 25) |11111111|11111111|11111111|0101 ffffff5 [28]
         * ( 26) |11111111|11111111|11111111|0110 ffffff6 [28]
         * ( 27) |11111111|11111111|11111111|0111 ffffff7 [28]
         * ( 28) |11111111|11111111|11111111|1000 ffffff8 [28]
         * ( 29) |11111111|11111111|11111111|1001 ffffff9 [28]
         * ( 30) |11111111|11111111|11111111|1010 ffffffa [28]
         * ( 31) |11111111|11111111|11111111|1011 ffffffb [28]
         * (127) |11111111|11111111|11111111|1100 ffffffc [28]
         * (220) |11111111|11111111|11111111|1101 ffffffd [28]
         * (249) |11111111|11111111|11111111|1110 ffffffe [28]
         */

        if (register == 0x0fffffe2) {
            hChar.setAsciiValue((byte) 2);
            return RESULT.YES;
        } else if (register == 0x0fffffe3) {
            hChar.setAsciiValue((byte) 3);
            return RESULT.YES;
        } else if (register == 0x0fffffe4) {
            hChar.setAsciiValue((byte) 4);
            return RESULT.YES;
        } else if (register == 0x0fffffe5) {
            hChar.setAsciiValue((byte) 5);
            return RESULT.YES;
        } else if (register == 0x0fffffe6) {
            hChar.setAsciiValue((byte) 6);
            return RESULT.YES;
        } else if (register == 0x0fffffe7) {
            hChar.setAsciiValue((byte) 7);
            return RESULT.YES;
        } else if (register == 0x0fffffe8) {
            hChar.setAsciiValue((byte) 8);
            return RESULT.YES;
        } else if (register == 0x0fffffe9) {
            hChar.setAsciiValue((byte) 11);
            return RESULT.YES;
        } else if (register == 0x0fffffea) {
            hChar.setAsciiValue((byte) 12);
            return RESULT.YES;
        } else if (register == 0x0fffffeb) {
            hChar.setAsciiValue((byte) 14);
            return RESULT.YES;
        } else if (register == 0x0fffffec) {
            hChar.setAsciiValue((byte) 15);
            return RESULT.YES;
        } else if (register == 0x0fffffed) {
            hChar.setAsciiValue((byte) 16);
            return RESULT.YES;
        } else if (register == 0x0fffffee) {
            hChar.setAsciiValue((byte) 17);
            return RESULT.YES;
        } else if (register == 0x0fffffef) {
            hChar.setAsciiValue((byte) 18);
            return RESULT.YES;
        } else if (register == 0x0ffffff0) {
            hChar.setAsciiValue((byte) 19);
            return RESULT.YES;
        } else if (register == 0x0ffffff1) {
            hChar.setAsciiValue((byte) 20);
            return RESULT.YES;
        } else if (register == 0x0ffffff2) {
            hChar.setAsciiValue((byte) 21);
            return RESULT.YES;
        } else if (register == 0x0ffffff3) {
            hChar.setAsciiValue((byte) 23);
            return RESULT.YES;
        } else if (register == 0x0ffffff4) {
            hChar.setAsciiValue((byte) 24);
            return RESULT.YES;
        } else if (register == 0x0ffffff5) {
            hChar.setAsciiValue((byte) 25);
            return RESULT.YES;
        } else if (register == 0x0ffffff6) {
            hChar.setAsciiValue((byte) 26);
            return RESULT.YES;
        } else if (register == 0x0ffffff7) {
            hChar.setAsciiValue((byte) 27);
            return RESULT.YES;
        } else if (register == 0x0ffffff8) {
            hChar.setAsciiValue((byte) 28);
            return RESULT.YES;
        } else if (register == 0x0ffffff9) {
            hChar.setAsciiValue((byte) 29);
            return RESULT.YES;
        } else if (register == 0x0ffffffa) {
            hChar.setAsciiValue((byte) 30);
            return RESULT.YES;
        } else if (register == 0x0ffffffb) {
            hChar.setAsciiValue((byte) 31);
            return RESULT.YES;
        } else if (register == 0x0ffffffc) {
            hChar.setAsciiValue((byte) 127);
            return RESULT.YES;
        } else if (register == 0x0ffffffd) {
            hChar.setAsciiValue((byte) 220);
            return RESULT.YES;
        } else if (register == 0x0ffffffe) {
            hChar.setAsciiValue((byte) 249);
            return RESULT.YES;
        }

        return RESULT.NO;
    }

    public static RESULT check30(int register, HuffmanChar hChar) {
        /*
         * ( 10) |11111111|11111111|11111111|111100 3ffffffc [30]
         * ( 13) |11111111|11111111|11111111|111101 3ffffffd [30]
         * ( 22) |11111111|11111111|11111111|111110 3ffffffe [30]
         * EOS (256) |11111111|11111111|11111111|111111 3fffffff [30]
         */

        if (register == 0x3ffffffc) {
            hChar.setAsciiValue((byte) 10);
            return RESULT.YES;
        } else if (register == 0x3ffffffd) {
            hChar.setAsciiValue((byte) 13);
            return RESULT.YES;
        } else if (register == 0x3ffffffe) {
            hChar.setAsciiValue((byte) 22);
            return RESULT.YES;
        } else if (register == 0x3fffffff) {
            hChar.setAsciiValue((byte) 256);
            return RESULT.EOS;
        }

        return RESULT.NO;
    }

}
