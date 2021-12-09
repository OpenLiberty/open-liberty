/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

public abstract class BuzzHash
{
    private static final String CLASS_NAME = "com.ibm.ejs.container.util.BuzzHash";

    //
    // The algorithm used to compute a hash code for the byte array is an
    // adaptation of the "buzHash" algorithm presented in "Hashing Concepts
    // and the Java Programming Language" by Robert Uzgalis, Computer Science
    // Department University of Auckland, Auckland, New Zealand (1996)
    //
    public static final long computeHashCode(String str)
    {
        // d179573 Begins
        return computeHashCode(str, false);
    }

    // The original version of the BuzzHash algorithm is desigend to best suited
    //  for hashing String that is less than 65 characters. Due to the nature of
    //  the algorithm, for each hashing character the hash code is rotated by one
    //  bit and xor'ed with a mix character to obtain a new value for the iteration.
    //  We may run into a duplicate hash value if the following conditions are met:
    //   1) If we have 2 Strings whose starting hashcode are the same,
    //   2) 1st character is the same as the 65th character &
    //   3) all the 63 characters in between are the same between the 2 Strings.
    //  E.g. if A(......63 characters.....)A yields hashcode X
    //     then B(...same 63 characters...)B will also yeilds hashcode X
    // The 64 characters length associated to the size of a primitive long in the
    //  Java programming language. I.e. If the implementation uses a 32 bit mixChar
    //  and return a 32 bit hashcode then condition 3) will be a 31 characters span.
    //
    // In recognizing the nature of the short coming of the hasing algorithm, we
    //  have modified the algorithm to perform another level of mixing based on the
    //  value of the hashing character IF and ONLY IF the ( n ) and ( n + 64 ) characters
    //  are the same.
    public static final long computeHashCode(String str, boolean modifyAlgorithm)
    {
        // d179573 Ends
        // to avoid EBCDIC / ASCII / Unicdoe encoding difference (i.e. z/OS390)
        //  uses toCharArray() instead of getBytes().
        char chars[] = str.toCharArray();

        // Corrected the algorithm to more closely match the 'buzHash'
        // algorithm it is patterned from and produce both a 64 and 32
        // bit value (64 bit value may be better).
        long h = initial_hash;
        for (int i = 0; i < chars.length; ++i)
        { // d179573 Begins
            char thisChar = chars[i];
            if (modifyAlgorithm)
            {
                // for all recurrences of the same character in a multiple 64 positions
                //  before the current index, need to mix/scramble the hash to another iteration.
                // d183360 Begins
                int j = i;
                while (j > 0)
                {
                    if (j >= 64 && thisChar == chars[j - 64])
                    {
                        thisChar ^= mix_primary[thisChar & 0xff];
                    }
                    j -= 64;
                }
                // d183360 Ends
            }
            h = ((h << 1) | (h >>> 63)) ^ mix_primary[thisChar & 0xff];
            // d179573 Ends
        }
        return h;
    }

    public static final String computeHashStringMid32Bit(String str)
    {
        // d179573 Begins
        return computeHashStringMid32Bit(str, false);
    }

    public static final String computeHashStringMid32Bit(String str, boolean modifyAlgorithm)
    {
        long hCode = (computeHashCode(str, modifyAlgorithm) >> 32) & 0xffffffff;
        // d179573 Ends
        char[] hashStr = new char[8];
        for (int i = 7; i >= 0; --i)
        {
            hashStr[i] = "0123456789abcdef".charAt((int) (hCode & 15));
            hCode >>>= 4;
        }
        return new String(hashStr);
    }

    public static final String computeHashString(String str)
    {
        // d179573 Begins
        return computeHashString(str, false);
    }

    public static final String computeHashString(String str, boolean modifyAlgorithm)
    {
        long hCode = computeHashCode(str, modifyAlgorithm);
        // d179573 Ends
        char[] hashStr = new char[16];
        for (int i = 15; i >= 0; --i)
        {
            hashStr[i] = "0123456789abcdef".charAt((int) (hCode & 15));
            hCode >>>= 4;
        }
        return new String(hashStr);
    }

    public static void main(String args[])
    {
        System.out.println(computeHashStringMid32Bit(args[0], args.length > 1));
    }

    /////////////////////////////////////////////////////////////////////////
    //
    // Data
    //

    // Initial value, and specially selected random numbers based on the
    // 'buzHash' algorithm.

    private static long initial_hash = 0xe12398c6d9ae3b8aL;
    private static long mix_primary[/* 0:255 */] = {
                                                   /* 000 */0x4476081a7043a46fL, 0x45768b8a6e7eac19L, 0xebd556c1cf055952L,
                                                   /*     */0x72ed2da1bf010101L, 0x3ff2030b128e8a64L,
                                                   /* 005 */0xcbc330238adcfef2L, 0x737807fe42e20c6cL, 0x74dabaedb1095c58L,
                                                   /*     */0x968f065c65361d67L, 0xd3f4018ac7a4b199L,
                                                   /* 010 */0x954b389b52f24df2L, 0x2f97a9d8d0549327L, 0xb9bea2b49a3b180fL,
                                                   /*     */0xaf2f42536b21f2ebL, 0x85d991663cff1325L,
                                                   /* 015 */0xb9e1260207b575b9L, 0xf3ea88398a23b7e2L, 0xfaf8c83ffbd9091dL,
                                                   /*     */0x4274fe90834dbdf9L, 0x3f20b157b68d6313L,
                                                   /* 020 */0x68b48972b6d06b93L, 0x694837b6eba548afL, 0xeecb51d1acc917c9L,
                                                   /*     */0xf1c633f02dffbcfaL, 0xa6549ec9d301f3b5L,
                                                   /* 025 */0x451dc944f1663592L, 0x446d6acef6ce9e4fL, 0x1c8a5b3013206f02L,
                                                   /*     */0x5908ca36f2dc50f7L, 0x4fd55d3f3e880a87L,
                                                   /* 030 */0xa03a8dbeabbf065dL, 0x3ccbbe078fabcb6dL, 0x1da53a259116f2d0L,
                                                   /*     */0xfb27a96fcb9af152L, 0x50aba242e85aec09L,
                                                   /* 035 */0x24d4e414fc4fc987L, 0x83971844a9ce535eL, 0xc26a3fdeb849398eL,
                                                   /*     */0xc2380d044d2e70d8L, 0xab418aa8ae19b18fL,
                                                   /* 040 */0xd95b6b9247d5ebeaL, 0x8b3b2171fdc60511L, 0xe15cd0ae3fcc44afL,
                                                   /*     */0x5a4e27f914a68f17L, 0x377bd28ca09aafdcL,
                                                   /* 045 */0xbbeb9828594a3294L, 0x7c8df263ae1de1b9L, 0xba0a48a5fd1c1dd0L,
                                                   /*     */0x57cc1b8818b98ee6L, 0x8c570975d357dabcL,
                                                   /* 050 */0x76bdcd6f2e8826aaL, 0x529b15b6ec4055f1L, 0x9147c7a54c34f8a9L,
                                                   /*     */0x2f96a7728170e402L, 0xe46602f455eca72eL,
                                                   /* 055 */0x22834c4dd1bde03fL, 0x2644cf5a25e368ffL, 0x907c6de90b120f4aL,
                                                   /*     */0xadfe8ba99028f728L, 0xa85199ae14df0433L,
                                                   /* 060 */0x2d749b946dd3601eL, 0x76e35457aa052772L, 0x90410bf6e427f736L,
                                                   /*     */0x536ad04d13e35041L, 0x8cc0d76769b76914L,
                                                   /* 065 */0xae0249f6e3b3c01cL, 0x1bdfd075307d6fafL, 0xd8e04f70c221deccL,
                                                   /*     */0x4ab23622a4281a5dL, 0x37a5613da2fcaba7L,
                                                   /* 070 */0x19a56203666d4a9fL, 0x158ffab502c4be93L, 0x0bee714e332ecb2fL,
                                                   /*     */0x69b71a59f6f74ab0L, 0x0fc7fc622f1dfe8fL,
                                                   /* 075 */0x513966de7152a6f9L, 0xc16fae9cc2ea9be7L, 0xb66f0ac586c1899eL,
                                                   /*     */0x11e124aee3bdefd7L, 0x86cf5a577512901bL,
                                                   /* 080 */0x33f33ba6994a1fbdL, 0xde6c4d1d3d47ff0dL, 0x6a99220dc6f78e66L,
                                                   /*     */0x2dc06ca93e2d25d2L, 0x96413b520134d573L,
                                                   /* 085 */0xb4715ce8e1023afaL, 0xe6a75900c8c66c0aL, 0x6448f13ad54c12edL,
                                                   /*     */0xb9057c28cf6689f0L, 0xf4023daf67f7677aL,
                                                   /* 090 */0x877c2650767b9867L, 0xb7ea587dcd5b2341L, 0xc048cf111733f9bcL,
                                                   /*     */0x112012c15bc867bfL, 0xc95f52b1d9418811L,
                                                   /* 095 */0xa47e624ee7499083L, 0x26928606df9b12e8L, 0x5d020462ec3e0928L,
                                                   /*     */0x8bbde651f6d08914L, 0xd5db83db758e524aL,
                                                   /* 100 */0x3105e355c000f455L, 0xdd7fe1b81a786c79L, 0x1f3a818c8e012db1L,
                                                   /*     */0xd902de819d7b42faL, 0x4200e63325cda5f0L,
                                                   /* 105 */0x0e919cdc5fba9220L, 0x5360dd54605a11e1L, 0xa3182d0e6cb23e6cL,
                                                   /*     */0x13ee462c1b483b87L, 0x1b1b6087b997ee22L,
                                                   /* 110 */0x81c36d0b877f7362L, 0xc24879932c1768d4L, 0x1faa756e1673f9adL,
                                                   /*     */0x61651b24d11fe93dL, 0x30fe3d9304e1cde4L,
                                                   /* 115 */0x7be867c750747250L, 0x973e52c7005b5db6L, 0x75d6b699bbaf4817L,
                                                   /*     */0x25d2a9e97379e196L, 0xe65fb599aca98701L,
                                                   /* 120 */0x6ac27960d24bde84L, 0xdfacc04c9fabbcb6L, 0xa46cd07f4a97882bL,
                                                   /*     */0x652031d8e59a1fd8L, 0x1185bd967ec7ce10L,
                                                   /* 125 */0xfc9bd84c6780f244L, 0x0a0c59872f61b3ffL, 0x63885727a1c71c95L,
                                                   /*     */0x5e88b4390b2d765cL, 0xf0005ccaf988514dL,
                                                   /* 130 */0x474e44280a98e840L, 0x32de151c1411bc42L, 0x2c4b86d5aa4482c2L,
                                                   /*     */0xccd93deb2d9d47daL, 0x3743236ff128a622L,
                                                   /* 135 */0x42ed2f2635ba5647L, 0x99c74afd18962dbdL, 0x2d663bb870f6d242L,
                                                   /*     */0x7912033bc7635d81L, 0xb442862f43753680L,
                                                   /* 140 */0x94b1a5400aeaab4cL, 0x5ce285fe810f2220L, 0xe8a7dbe565d9c0b1L,
                                                   /*     */0x219131af78356c94L, 0x7b3a80d130f27e2fL,
                                                   /* 145 */0xbaa5d2859d16b440L, 0x821cfb6935771070L, 0xf68cfb6ee9bc2336L,
                                                   /*     */0x18244132e935d2fdL, 0x2ed0bda1f4720cffL,
                                                   /* 150 */0x4ed48cdf6975173cL, 0xfd37a7a2520e2405L, 0x82c102b2a9e73ce2L,
                                                   /*     */0xadac6517062623a7L, 0x5a1294d318e26104L,
                                                   /* 155 */0xea84fe65c0e4f061L, 0x4f96f8a9464cfee9L, 0x9831dff8ccdc534aL,
                                                   /*     */0x4ca927cd0f192a14L, 0x030900b294b71649L,
                                                   /* 160 */0x644b263b9aeb0675L, 0xa601d4e34647e040L, 0x34d897eb397f1004L,
                                                   /*     */0xa6101c37f4ec8dfcL, 0xc29d2a8bbfd0006bL,
                                                   /* 165 */0xc6b07df8c5b4ed0fL, 0xce1b7d92ba6bccbeL, 0xfa2f99442e03fe1bL,
                                                   /*     */0xd8863e4c16f0b363L, 0x033b2cccc3392942L,
                                                   /* 170 */0x757dc33522d6cf9cL, 0xf07b1ff6ce55fec5L, 0x1569e75f09b40463L,
                                                   /*     */0xfa33fa08f14a310bL, 0x6eb79aa27bbcf76bL,
                                                   /* 175 */0x157061207c249602L, 0x25e5a71fc4e99555L, 0x5df1fe93de625355L,
                                                   /*     */0x235b56090c1aa55dL, 0xe51068613eaced91L,
                                                   /* 180 */0x45bd47b893b9ff1eL, 0x6595e1798d381f2dL, 0xc9b5848cbcdb5ba8L,
                                                   /*     */0x65985146ff7792bcL, 0x4ab4a17bf05a19a0L,
                                                   /* 185 */0xfd94f4ca560ffb0cL, 0xcf9bad581a68fa68L, 0x92b4f0b502b1ce1aL,
                                                   /*     */0xbcbec0769a610474L, 0x8dbd31ded1a0fecbL,
                                                   /* 190 */0xdd1f5ed9f90e8533L, 0x61c1e6a523f84d95L, 0xf24475f383c110c4L,
                                                   /*     */0xdb2dffa66f90588dL, 0xac06d88e9ee04455L,
                                                   /* 195 */0xa215fc47c40504baL, 0x86d7caebfee93369L, 0x9eaec31985804099L,
                                                   /*     */0x0fba2214abe5d01bL, 0x5a32975a4b3865d6L,
                                                   /* 200 */0x8cceebc98a5c108fL, 0x7e12c4589654f2dcL, 0xa49ad49fb0d19772L,
                                                   /*     */0x3d142dd9c406152bL, 0x9f13589e7be2b8a5L,
                                                   /* 205 */0x5e8dbac1892967adL, 0xcc23b93a6308e597L, 0x1ef35f5fe874e16aL,
                                                   /*     */0x63ae9cc08d2e274fL, 0x5bbabee56007fc05L,
                                                   /* 210 */0xabfd72994230fc39L, 0x9d71a13a99144de1L, 0xd9daf5aa8dcc89b3L,
                                                   /*     */0xe145ec0514161bfdL, 0x143befc2498cd270L,
                                                   /* 215 */0xa8e192557dbbd9f8L, 0xcbeda2445628d7d0L, 0x997f0a93205d9ea4L,
                                                   /*     */0x01014a97f214ebfaL, 0x70c026ffd1ebedafL,
                                                   /* 220 */0xf8737b1b3237002fL, 0x8afcbef3147e6e5eL, 0x0e1bb0684483ebd3L,
                                                   /*     */0x4cbad70ae9b05aa6L, 0xd4a31f523517c363L,
                                                   /* 225 */0xdb0f057ae8e9e8a2L, 0x400894a919d89df6L, 0x6a626a9b62defab3L,
                                                   /*     */0xf907fd7e14f4e201L, 0xe10e4a5657c48f3fL,
                                                   /* 230 */0xb17f9f54b8e6e5dcL, 0x6b9e69045fa6d27aL, 0x8b74b6a41dc3078eL,
                                                   /*     */0x027954d45ca367f9L, 0xd07207b8fdcbb7ccL,
                                                   /* 235 */0xf397c47d2f36414bL, 0x05e4e8b11d3a034fL, 0x36adb3f7122d654fL,
                                                   /*     */0x607d9540eb336078L, 0xb639118e3a8b9600L,
                                                   /* 240 */0xd0a406770b5f1484L, 0x3cbee8213ccfb7c6L, 0x467967bb2ff89cf1L,
                                                   /*     */0xb115fe29609919a6L, 0xba740e6ffa83287eL,
                                                   /* 245 */0xb4e51be9b694b7cdL, 0xc9a081c677df5aeaL, 0x2e1fbcd8944508ccL,
                                                   /*     */0xf626e7895581fbb8L, 0x3ce6e9b5728a05cbL,
                                                   /* 250 */0x46e87f2664a31712L, 0x8c1dc526c2f6acfaL, 0x7b4826726e560b10L,
                                                   /*     */0x2966e0099d8d7ce1L, 0xbb0dd5240d2b2adeL, 0x0d527cc60bbaa936L
    };

}
