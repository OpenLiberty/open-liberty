/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.encryption;

/**
 *
 */
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

final class AuditCrypto {

    private static TraceComponent tc = Tr.register(AuditCrypto.class, null, "com.ibm.ejs.resources.security");
    private static final String CRYPTO_ALGORITHM = "RSA";
    private static final String ENCRYPT_ALGORITHM = "DESede";
    private static final String CIPHER = "DESede/ECB/PKCS5Padding";
    private static IvParameterSpec ivs8 = null;
    private static IvParameterSpec ivs16 = null;
    private static SecureRandom random = null;

    static final boolean cmp(byte[] b1, int off1, byte[] b2, int off2, int n) {
        while (--n >= 0)
            if (b1[off1++] != b2[off2++])
                return false;
        return true;
    }

    static final int msbf(byte[] data, int off, int n) {
        int v = 0;
        do {
            v |= (data[off++] & 0xFF) << ((--n) * 8);
        } while (n > 0);
        return v;
    }

    static final int msbf2(byte[] data, int i) {
        return (((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF));
    }

    static final void msbf(int v, byte[] data, int off, int n) {
        do {
            data[off++] = (byte) (v >>> ((--n) * 8));
        } while (n > 0);
    }

    static final void msbf4(int v, byte[] data, int i) {
        data[i] = (byte) (v >>> 24);
        data[i + 1] = (byte) (v >>> 16);
        data[i + 2] = (byte) (v >>> 8);
        data[i + 3] = (byte) v;
    }

    static final void msbf2(int v, byte[] data, int i) {
        data[i] = (byte) (v >>> 8);
        data[i + 1] = (byte) v;
    }

    static final int lsbf(byte[] data, int i, int n) {
        int v = 0;
        do {
            v |= (data[i + (--n)] & 0xFF) << n * 8;
        } while (n > 0);
        return v;
    }

    static final int lsbf4(byte[] data, int i) {
        return (data[i] & 0xFF) | ((data[i + 1] & 0xFF) << 8) |
               ((data[i + 2] & 0xFF) << 16) | (data[i + 3] << 24);
    }

    static final void lsbf4(int v, byte[] data, int i) {
        data[i] = (byte) v;
        data[i + 1] = (byte) (v >>> 8);
        data[i + 2] = (byte) (v >>> 16);
        data[i + 3] = (byte) (v >>> 24);
    }

    static void lsbf2(int v, byte[] data, int i) {
        data[i] = (byte) v;
        data[i + 1] = (byte) (v >>> 8);
    }

    private static double[] ETB = new double[16];
    static {
        double d = ETB[0] = 0.001;
        double log2d = Math.log(2 * d);
        int i = 1;
        do {
            ETB[i] = Math.exp(log2d / ++i) / 2;
        } while (i < ETB.length);
    }

    private static int slot, channels;
    private static int[] samples = new int[56];
    private static int[] ones = new int[16];
    private static int[] block = new int[16];

    static final void trng(byte[] to, int off, int len) {
        long accu = 0;
        int bits = 0, i, m, j;

        while (len-- > 0) {
            while (bits < 8) {

                int s = 0;
                do {
                    long t = System.currentTimeMillis();
                    while (System.currentTimeMillis() == t)
                        s++;
                } while (s == 0);

                int xor = samples[slot] ^ s;
                samples[slot] = s;

                i = 0;
                m = 1;
                do {

                    if ((xor & m) != 0) {
                        ones[i] += (((s & m) != 0) ? 1 : -1);
                        channels ^= m;
                    }

                    if (--block[i] == 0) {
                        accu = (accu << 1) | (((channels & m) != 0) ? 1 : 0);
                        bits++;
                    }
                    if (block[i] <= 0) {
                        for (j = 0; j < 16; j++) {
                            if (Math.abs(0.5 - (double) ones[i] / (double) 56) <= ETB[j])
                                break;
                        }

                        block[i] = (j == 16) ? -1 : j + 1;
                    }
                    m <<= 1;
                } while (++i < 16);
                slot = (slot + 1) % 56;
            }
            to[off++] = (byte) (accu >>> (bits -= 8));
        }
    }

    private static byte[] seed = new byte[32];
    private static int ri;
    private static boolean seedInitialized = false;

    static int trMix = 128;

    static final void random(byte[] to, int off, int n) {

        if (!seedInitialized) {
            trng(seed, 0, 32);
            md5(null, seed, 0, 32, seed, 0);

            rsaKeys = new byte[4][][];
            for (int i = 0, j = 0; i < rsaKeyMaterial.length; i++, j += 2) {
                rsaKeys[j] = new byte[8][];
                rsaKeys[j][2] = new BigInteger(rsaKeyMaterial[i][2], 36).toByteArray();
                rsaKeys[j][3] = new BigInteger(rsaKeyMaterial[i][0], 36).toByteArray();
                rsaKeys[j][4] = new BigInteger(rsaKeyMaterial[i][1], 36).toByteArray();
                setRSAKey(rsaKeys[j]);
                rsaKeys[j + 1] = new byte[][] { rsaKeys[j][0], rsaKeys[j][2] };
            }

            dsaKeys = new byte[4][4][];
            for (int i = 0, j = 0; i < dsaKeyMaterial.length; i++, j += 2) {

                for (int k = 0; k < 3; k++)
                    dsaKeys[j][k] = dsaKeys[j + 1][k] = new BigInteger(dsaKeyMaterial[i][k], 36).toByteArray();
                dsaKeys[j][3] = new BigInteger(dsaKeyMaterial[i][3], 36).toByteArray();
                dsaKeys[j + 1][3] = new BigInteger(dsaKeyMaterial[i][4], 36).toByteArray();
            }

            seedInitialized = true;
        }

        synchronized (seed) {
            for (int i = 0; i < n; i++) {
                int ri8 = ++ri % 8;

                if (ri % trMix == 0) {
                    byte b = seed[ri8];
                    trng(seed, ri8, 1);
                    seed[ri8] ^= b;
                }

                if (ri8 == 0)
                    md5(null, seed, 0, 32, seed, 0);

                to[off++] = seed[ri8];
            }
        }
    }

    static final void sha(int[] state, byte[] data, int off, int len, byte[] to, int pos) {
        int A, B, C, D, E;
        {
            A = 0x67452301;
            B = 0xEFCDAB89;
            C = 0x98BADCFE;
            D = 0x10325476;
            E = 0xC3D2E1F0;
        }

        final int[] W = new int[80];
        int X, i, n = len / 4;

        boolean done = false;
        boolean padded = false;

        do {

            for (i = 0; (i < 16) && (n > 0); n--, off += 4, i++)
                W[i] = (data[off + 3] & 0xFF) | ((data[off + 2] & 0xFF) << 8) |
                       ((data[off + 1] & 0xFF) << 16) | (data[off] << 24);

            if (i < 16) {

                if (!padded) {
                    W[i++] = ((X = len % 4) != 0) ? ((msbf(data, off, X) << ((4 - X) * 8)) | (1 << (31 - X * 8))) : (1 << 31);
                    if (i == 15)
                        W[15] = 0;
                    padded = true;
                }
                if (i <= 14) {
                    while (i < 14)
                        W[i++] = 0;

                    if (state != null)
                        len += state[5];
                    W[14] = len >>> 29;
                    W[15] = len << 3;
                    done = true;
                }
                i = 16;
            }
            do {
                W[i] = ((X = W[i - 3] ^ W[i - 8] ^ W[i - 14] ^ W[i - 16]) << 1) | (X >>> 31);
            } while (++i < 80);

            int A0 = A;
            int B0 = B;
            int C0 = C;
            int D0 = D;
            int E0 = E;
            i = 0;
            do {
                X = ((A << 5) | (A >>> 27)) + ((B & C) | (~B & D)) + E + W[i] + 0x5A827999;
                E = D;
                D = C;
                C = (B << 30) | (B >>> 2);
                B = A;
                A = X;
            } while (++i < 20);
            do {
                X = ((A << 5) | (A >>> 27)) + (B ^ C ^ D) + E + W[i] + 0x6ED9EBA1;
                E = D;
                D = C;
                C = (B << 30) | (B >>> 2);
                B = A;
                A = X;
            } while (++i < 40);
            do {
                X = ((A << 5) | (A >>> 27)) + ((B & C) | ((B | C) & D)) + E + W[i] + 0x8F1BBCDC;
                E = D;
                D = C;
                C = (B << 30) | (B >>> 2);
                B = A;
                A = X;
            } while (++i < 60);
            do {
                X = ((A << 5) | (A >>> 27)) + (B ^ C ^ D) + E + W[i] + 0xCA62C1D6;
                E = D;
                D = C;
                C = (B << 30) | (B >>> 2);
                B = A;
                A = X;
            } while (++i < 80);
            A += A0;
            B += B0;
            C += C0;
            D += D0;
            E += E0;
        } while (!done);
        {
            msbf4(A, to, pos);
            msbf4(B, to, pos + 4);
            msbf4(C, to, pos + 8);
            msbf4(D, to, pos + 12);
            msbf4(E, to, pos + 16);
        }
    }

    private static final int FF(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += ((b & c) | (~b & d)) + x + ac) << l) | (a >>> r)) + b;
    }

    private static final int GG(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += ((b & d) | (c & ~d)) + x + ac) << l) | (a >>> r)) + b;
    }

    private static final int HH(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += (b ^ c ^ d) + x + ac) << l) | (a >>> r)) + b;
    }

    private static final int II(int a, int b, int c, int d, int x, int l, int r, int ac) {
        return (((a += (c ^ (b | ~d)) + x + ac) << l) | (a >>> r)) + b;
    }

    static final void md5(int[] state, byte[] data, int off, int len, byte[] to, int pos) {
        int a, b, c, d;
        {
            a = 0x67452301;
            b = 0xefcdab89;
            c = 0x98badcfe;
            d = 0x10325476;
        }

        int W0, W1, W2, W3, W4, W5, W6, W7, W8, W9, W10, W11, W12, W13, W14, W15;
        int i, n = len / 4, a0, b0, c0, d0;
        final int[] W = new int[16];

        boolean done = false;
        boolean padded = false;

        do {

            for (i = 0; (i < 16) && (n > 0); n--, off += 4)
                W[i++] = lsbf4(data, off);

            if (i < 16) {

                if (!padded) {
                    W[i++] = ((a0 = len % 4) != 0) ? (lsbf(data, off, a0) | (0x80 << (a0 * 8))) : 0x80;
                    if (i == 15)
                        W[15] = 0;
                    padded = true;
                }
                if (i <= 14) {
                    while (i < 14)
                        W[i++] = 0;

                    if (state != null)
                        len += state[5];
                    W[14] = len << 3;
                    W[15] = len >>> 29;
                    done = true;
                }
            }

            a = FF((a0 = a), b, c, d, (W0 = W[0]), 7, 25, 0xd76aa478);
            d = FF((d0 = d), a, b, c, (W1 = W[1]), 12, 20, 0xe8c7b756);
            c = FF((c0 = c), d, a, b, (W2 = W[2]), 17, 15, 0x242070db);
            b = FF((b0 = b), c, d, a, (W3 = W[3]), 22, 10, 0xc1bdceee);
            a = FF(a, b, c, d, (W4 = W[4]), 7, 25, 0xf57c0faf);
            d = FF(d, a, b, c, (W5 = W[5]), 12, 20, 0x4787c62a);
            c = FF(c, d, a, b, (W6 = W[6]), 17, 15, 0xa8304613);
            b = FF(b, c, d, a, (W7 = W[7]), 22, 10, 0xfd469501);
            a = FF(a, b, c, d, (W8 = W[8]), 7, 25, 0x698098d8);
            d = FF(d, a, b, c, (W9 = W[9]), 12, 20, 0x8b44f7af);
            c = FF(c, d, a, b, (W10 = W[10]), 17, 15, 0xffff5bb1);
            b = FF(b, c, d, a, (W11 = W[11]), 22, 10, 0x895cd7be);
            a = FF(a, b, c, d, (W12 = W[12]), 7, 25, 0x6b901122);
            d = FF(d, a, b, c, (W13 = W[13]), 12, 20, 0xfd987193);
            c = FF(c, d, a, b, (W14 = W[14]), 17, 15, 0xa679438e);
            b = FF(b, c, d, a, (W15 = W[15]), 22, 10, 0x49b40821);

            a = GG(a, b, c, d, W1, 5, 27, 0xf61e2562);
            d = GG(d, a, b, c, W6, 9, 23, 0xc040b340);
            c = GG(c, d, a, b, W11, 14, 18, 0x265e5a51);
            b = GG(b, c, d, a, W0, 20, 12, 0xe9b6c7aa);
            a = GG(a, b, c, d, W5, 5, 27, 0xd62f105d);
            d = GG(d, a, b, c, W10, 9, 23, 0x2441453);
            c = GG(c, d, a, b, W15, 14, 18, 0xd8a1e681);
            b = GG(b, c, d, a, W4, 20, 12, 0xe7d3fbc8);
            a = GG(a, b, c, d, W9, 5, 27, 0x21e1cde6);
            d = GG(d, a, b, c, W14, 9, 23, 0xc33707d6);
            c = GG(c, d, a, b, W3, 14, 18, 0xf4d50d87);
            b = GG(b, c, d, a, W8, 20, 12, 0x455a14ed);
            a = GG(a, b, c, d, W13, 5, 27, 0xa9e3e905);
            d = GG(d, a, b, c, W2, 9, 23, 0xfcefa3f8);
            c = GG(c, d, a, b, W7, 14, 18, 0x676f02d9);
            b = GG(b, c, d, a, W12, 20, 12, 0x8d2a4c8a);

            a = HH(a, b, c, d, W5, 4, 28, 0xfffa3942);
            d = HH(d, a, b, c, W8, 11, 21, 0x8771f681);
            c = HH(c, d, a, b, W11, 16, 16, 0x6d9d6122);
            b = HH(b, c, d, a, W14, 23, 9, 0xfde5380c);
            a = HH(a, b, c, d, W1, 4, 28, 0xa4beea44);
            d = HH(d, a, b, c, W4, 11, 21, 0x4bdecfa9);
            c = HH(c, d, a, b, W7, 16, 16, 0xf6bb4b60);
            b = HH(b, c, d, a, W10, 23, 9, 0xbebfbc70);
            a = HH(a, b, c, d, W13, 4, 28, 0x289b7ec6);
            d = HH(d, a, b, c, W0, 11, 21, 0xeaa127fa);
            c = HH(c, d, a, b, W3, 16, 16, 0xd4ef3085);
            b = HH(b, c, d, a, W6, 23, 9, 0x4881d05);
            a = HH(a, b, c, d, W9, 4, 28, 0xd9d4d039);
            d = HH(d, a, b, c, W12, 11, 21, 0xe6db99e5);
            c = HH(c, d, a, b, W15, 16, 16, 0x1fa27cf8);
            b = HH(b, c, d, a, W2, 23, 9, 0xc4ac5665);

            a = II(a, b, c, d, W0, 6, 26, 0xf4292244);
            d = II(d, a, b, c, W7, 10, 22, 0x432aff97);
            c = II(c, d, a, b, W14, 15, 17, 0xab9423a7);
            b = II(b, c, d, a, W5, 21, 11, 0xfc93a039);
            a = II(a, b, c, d, W12, 6, 26, 0x655b59c3);
            d = II(d, a, b, c, W3, 10, 22, 0x8f0ccc92);
            c = II(c, d, a, b, W10, 15, 17, 0xffeff47d);
            b = II(b, c, d, a, W1, 21, 11, 0x85845dd1);
            a = II(a, b, c, d, W8, 6, 26, 0x6fa87e4f);
            d = II(d, a, b, c, W15, 10, 22, 0xfe2ce6e0);
            c = II(c, d, a, b, W6, 15, 17, 0xa3014314);
            b = II(b, c, d, a, W13, 21, 11, 0x4e0811a1);
            a = II(a, b, c, d, W4, 6, 26, 0xf7537e82);
            d = II(d, a, b, c, W11, 10, 22, 0xbd3af235);
            c = II(c, d, a, b, W2, 15, 17, 0x2ad7d2bb);
            b = II(b, c, d, a, W9, 21, 11, 0xeb86d391) + b0;

            a += a0;
            c += c0;
            d += d0;
        } while (!done);

        {
            lsbf4(a, to, pos);
            lsbf4(b, to, pos + 4);
            lsbf4(c, to, pos + 8);
            lsbf4(d, to, pos + 12);
        }
    }

    private static final byte[] Pmd2 = new byte[256];
    static {
        final String p = "\u0014\u004B\u0048\u003C\u004D\u000B\u0030\u007C" +
                         "\u0000\u004F\u0026\u0065\u0025\u0007\u0059\u0070" +
                         "\u0003\u0004\u006C\u002A\u0038\u0017\u0067\u0040" +
                         "\u0063\\\u0071\u0049\u0044\u004C\u0057\u0059" +
                         "\u005E\u0013\u0010\u002C\u0050\u007A\u0036\u0057" +
                         "\u001E\u003F\u003A\u004E\u0000\u0059\u004E\u0042" +
                         "\u0037\u0046\u0011\u0021\u003F\u0014\u0025\u003E" +
                         "\u0027\u0031\u001A\u006D\u0054\u007B\u003C\u0049" +
                         "\u0050\u003E\u007E\u0058\u0075\u006C\u005F\u006E" +
                         "\u003D\u002A\u002D\u0007\u004C\u0044\u002B\u0032" +
                         "\u0003\u004F\u0072\u004C\u0010\u0042\u0012\u000B" +
                         "\u0011\u0017\u0064\u0018\u0003\u007D\u003B\u001A" +
                         "\u002D\u0024\u0006\"\u0039\u0054\u007D\u004C" +
                         "\u0073\u006F\u007E\u0079\u0038\u000F\u007E\u0019" +
                         "\u0018\u002C\u0069\n\u002D\u0057\u0023\u0057" +
                         "\u002F\u0024\u0045\u002A\u0062\u005A\u0055\u0046" +
                         "\u0027\u006E\u0007\r\u0014\u005A\u0048\u007D" +
                         "\u005B\u001D\u005F\u0046\u005F\n\u0038\u0074" +
                         "\u0002\u003C\u0028\u0059\u006B\u0041\u0032\u0064" +
                         "\u0038\u0061\u0064\u0008\u0032\u006F\u001E\u0065" +
                         "\u0073\u000B\u0035\u0000\u0010\u006D\u0040\u0025" +
                         "\u0056\u006B\u0056\u000B\u004F\u0058\u0038\u0046" +
                         "\u0030\u005A\u0026\u0044\u0003\u0078\u001E\u0055" +
                         "\u0023\u0068\u0064\u003D\u006A\u0046\u005E\u003A" +
                         "\u0061\u0057\u001F\u001C\u0075\u006B\u000B\u006A" +
                         "\u0013\u000B\n\u0030\u006B\u003A\n\u0028" +
                         "\u0042\u0002\u003A\u003D\u007E\u0037\u0068\u0041" +
                         "\u0040\u0053\u002A\u0026\u0056\u0070\u006F\u0048" +
                         "\u0036\u0030\u0035\u003F\u0051\u0013\u0042\u007B" +
                         "\u0004\u0003\u0017\u005B\n\u0029\u0071\u0008" +
                         "\u004A\u0062\u007C\u0036\u001F\u0021\u005B\u0069" +
                         "\u0065\u0075\u003F\u0063\u0058\u0000\u003A\u0039" +
                         "\u0079\u003B\u0076\u0070\u0073\u0019\u0031\u0050" +
                         "\u0072\u0029\u004E\u0077\u0017\u0063\u0056\u0075" +
                         "\u0025\u0042\u0046\u0014\"\u0042\u0069\u000F" +
                         "\u0076\u0047\u0063\u002D\\\u0066\u001A\u0033" +
                         "\u004F\u0044\u0030\u0031\u0020";

        int i = 0;
        do {
            Pmd2[i] = getBits(p, i * 8, 8);
        } while (++i < 256);
    }

    static final void md2(byte[][] state, byte[] data, int off, int len, byte[] to, int pos) {
        byte[] C;
        byte[] X;
        int i, j;
        final byte[] p = Pmd2;
        byte t, f = 0;
        {
            C = new byte[16];
            X = new byte[48];
        }

        do {
            t = C[15];

            i = 0;
            do {
                if (len > 0) {
                    f = data[off++];
                    len--;
                } else if (len == 0) {
                    f = (byte) (16 - i);
                    len--;
                } else if (len == -2)
                    f = C[i];

                X[i + 32] = (byte) (X[i] ^ (X[i + 16] = f));

                t = (C[i] ^= p[(f ^ t) & 0xFF]);
            } while (++i < 16);

            if (len <= 0)
                len--;

            t = 0;
            i = 0;
            do {
                j = 0;
                do {
                    t = (X[j] ^= p[t & 0xFF]);
                } while (++j < 48);
                t += i;
            } while (++i < 18);
        } while (len > -3);
        System.arraycopy(X, 0, to, pos, 16);
    }

    static final byte[] rc4key(byte[] rawKey, int off, int len) {
        final byte[] S = new byte[256 + 2];
        int i = 0, j = 0;
        do {
            S[i] = (byte) i;
        } while (++i < 256);
        i = 0;
        do {
            j = (j + S[i] + rawKey[(i + off) % len]) & 0xFF;
            byte a = S[i];
            S[i] = S[j];
            S[j] = a;
        } while (++i < 256);
        return S;
    }

    static final void rc4(byte[] key, byte[] data, int off, int len, byte[] to, int pos) {
        byte a, b;

        if ((len += off) == off)
            return;

        int Si = key[256] & 0xFF;
        int Sj = key[257] & 0xFF;

        if (off == pos) {
            do {
                b = key[(Sj = (Sj + (a = key[(Si = (Si + 1) & 0xFF)])) & 0xFF)];
                to[off] = (byte) (data[off] ^ key[((key[Sj] = a) + (key[Si] = b)) & 0xFF]);
            } while (++off < len);
        } else {
            do {
                b = key[(Sj = (Sj + (a = key[(Si = (Si + 1) & 0xFF)])) & 0xFF)];
                to[pos++] = (byte) (data[off] ^ key[((key[Sj] = a) + (key[Si] = b)) & 0xFF]);
            } while (++off < len);
        }
        key[256] = (byte) Si;
        key[257] = (byte) Sj;
    }

    private static final int PC[] = new int[8 * 64];
    static {
        final String cd = "\u0008\u0075\u0001\u0031\u0029\u0059\u001B\u0001" +
                          "\u0028\"\u004E\u0000\u0024\u0060\u000C\u0002" +
                          "\u002D\u0054\u0040\u0020\u001C\u0003\r\u0011" +
                          "\u0038\u0040\u0013\u0055\u0006\u0020\u0036\u000E" +
                          "\u0001\u0024\u0020\u0019\u0040\u0021\u001B\u0001" +
                          "\n\u0049\u0034\u0048\u0009\u0058\u0002\u004B";

        int l, n = 0, o = 0, c = 0, i;
        do {
            if ((l = getBits(cd, c * 6, 6)) != 32) {
                i = 0;
                do {
                    if (((1 << n) & i) != 0)
                        PC[o + i] |= 1 << l;
                } while (++i < 64);
                if ((n = (n + 1) % 6) == 0)
                    o += 64;
            }
        } while (++c < 56);
    }

    static final int[] desKey(boolean encrypt, byte[] rawKey, int off, int len) {
        int[] pc = PC;
        final int key[] = new int[len * 4];
        int c, d, t, s, a, i, j, n = 0;
        boolean e = encrypt;

        do {
            c = lsbf4(rawKey, off + n);
            d = lsbf4(rawKey, off + n + 4);

            c ^= (t = (d >>> 4 ^ c) & 0x0f0f0f0f);
            d ^= t << 4;
            c ^= (t = (c << 18 ^ c) & 0xcccc0000) ^ t >>> 18;
            d ^= (t = (d << 18 ^ d) & 0xcccc0000) ^ t >>> 18;
            c ^= (t = (d >>> 1 ^ c) & 0x55555555);
            d ^= t << 1;
            d ^= (t = (c >>> 8 ^ d) & 0x00ff00ff);
            c ^= t << 8;
            c ^= (t = (d >>> 1 ^ c) & 0x55555555);
            d ^= t << 1;

            d = d << 16 & 0x00ff0000 | d & 0x0000ff00 | d >> 16 & 0xFF | c >> 4 & 0x0F000000;
            c &= 0x0fffffff;

            i = 0;
            do {
                a = ((0x7EFC >> i & 1) == 1) ? 2 : 1;

                s = pc[(c = (c >>> a | c << (28 - a)) & 0x0fffffff) & 0x3f] |
                    pc[0x040 | c >> 6 & 0x03 | c >> 7 & 0x3c] |
                    pc[0x080 | c >> 13 & 0x0f | c >> 14 & 0x30] |
                    pc[0x0C0 | c >> 20 & 0x01 | c >> 21 & 0x06 | c >> 22 & 0x38];

                t = pc[0x100 | (d = (d >>> a | d << (28 - a)) & 0x0fffffff) & 0x3f] |
                    pc[0x140 | d >> 7 & 0x03 | d >> 8 & 0x3c] |
                    pc[0x180 | d >> 15 & 0x3f] |
                    pc[0x1C0 | d >> 21 & 0x0f | d >> 22 & 0x30];

                key[(j = (encrypt ? n : (len - n - 8)) * 4 + (e ? i : (15 - i)) * 2)] = t << 16 | s & 0xffff;
                key[j + 1] = (s = s >>> 16 | t & 0xffff0000) << 4 | s >>> 28;
            } while (++i < 16);
            e ^= true;
        } while ((n += 8) < len);
        return key;
    }

    private static final int[] SP0 = new int[4 * 64], SP1 = new int[4 * 64];
    static {
        String sp = "\u0040\r\u0073\u0014\u0054\u0059\u0025\u001E" +
                    "\u004B\u000B\"\u0009\u0052\u0059\u0049\u0039" +
                    "\u0035\u0025\u0032\u0059\u0057\u007D\u0003\u0012" +
                    "\u0004\u0041\u0030\u0000\u0008\u0049\u0024\u0028" +
                    "\u0000\u0051\u0014\u0002\u0002\u004A\u0004\u0044" +
                    "\u0012\u0020\u0028\u0000\u0040\u0059\u0010\u0021" +
                    "\u0016\u0002\u0030\u0001\u0020\u0011\u002D\u0069" +
                    "\u0043\u0007\u0052\u0058\u0036\u0028\u006F\u007F" +
                    "\u0041\u0024\u000C\u0000\u0060\u0049\u0011\u0001" +
                    "\u0050\u0005\u0000\u0024\u0034\u0006\u0020\u0020" +
                    "\n\n\u0008\u0028\u000C\u0000\u0052\u000C" +
                    "\u0010\u0041\u0004\u0011\u0010\u0061\u0040\u0004" +
                    "\u0058\u0053\u0055\u0016\u002E\u0070\u0065\u0007" +
                    "\u0061\u0040\u0029\u0016\u004B\u001A\u0073\u0016" +
                    "\u003D\u0026\u0024\u0051\u0004\u0056\r\u004A" +
                    "\u001B\u006A\"\u0067\u004F\u0004\u0005\u0014" +
                    "\u006C\u000E\u006D\u0046\u005B\u0023\u0028\u0033" +
                    "\u0010\u0029\u0072\u0045\u0047\r\u0070\u004B" +
                    "\u0063\u000E\u0020\u0000\u0026\u004A\u0028\u006D" +
                    "\u004D\u0013\u0014\u0036\u005A\u0064\u0000\u0004" +
                    "\u0046\u001A\u002D\u001D\u001C\u0079\u0026\u001C" +
                    "\u0056\u0010\u000E\u0038\u0061\u0071\u0053\u0049" +
                    "\u0031\u0025\u0072\u0060\u0015\u004D\u0039\u0014" +
                    "\u0053\u0034\u0052\u0046\u006C\u0068";

        final String sp1 = "\u0001\u0027\u0025\u0064\u004B\u0027\u0027\u0024" +
                           "\u004E\u0018\u0064\u0009\u0012\u0079\u0047\u0061" +
                           "\u0034\u0069\u0013\u0047\u0048\u0000\u0011\u004B" +
                           "\u0023\u0047\u0016\u0023\u0027\u0026\u0066\u004C" +
                           "\u0000\u0002\u000B\u0016\u001D\u000F\u000C\u001C" +
                           "\u001F\u0039\u0043\u0014\u0001\u0063\u002D\u0016" +
                           "\u0052\u0058\u0070\u0056\u001C\u0078\u0041\u0047" +
                           "\u0014\u005A\u002B\u0029\u000C\u006B\u0052\u0065" +
                           "\u007F\u0050\u0009\u0018\u0021\u0000\u0030\u0038" +
                           "\u0000\u0042\u0028\u0020\u0009\u0010\u0010\u0018" +
                           "\u0023\u0010\u0020\u002C\u0011\u0041\u0002\u0012" +
                           "\u0009\u0021\u0004\u0018\u0003\u0020\u0029\u0004" +
                           "\u0004\u0005\u005D\u0014\u0054\u005E\u0072\u0046" +
                           "\u0069\u002D\u0002\u0008\u0070\u0059\u0063\u006C" +
                           "\u0074\u0058\u0002\u006B\u003C\u0001\u0052\u003C" +
                           "\u006C\u0049\u004B\u0016\u004C\u0039\u0046\u0008" +
                           "\r\u001B\u0006\u005F\u0013\u0018\u0053\u001A" +
                           "\u002E\u0044\u0019\u002C\u002B\u0016\u0035\u0014" +
                           "\u0016\u0066\u004C\u007F\u007C\u0001\u0044\u0004" +
                           "\u0002\u0048\u0040\u0042\u0049\u0042\u0008\u0001" +
                           "\n\u0002\u0004\u0029\u0021\u0004\u000C\u0000" +
                           "\u0034\u0060\u0021\u0006\u000C\u0041\u0011\u0010" +
                           "\u0060\u0012\u0042\u0002\u0005\u0016\u003C\u006C" +
                           "\u0018\u004E\u001A\u0069\u0035\u0014";

        for (int[] st = SP0;;) {
            int a = 0, n, i, j = 63, r = 0, c = 0;
            long v = 0;
            n = 3;
            do {
                i = 0;
                do {
                    if (v == 0) {

                        if ((c = getBits(sp, a, 8) & 0xFF) == 0) {
                            a += 8;
                            i += 64;
                            continue;
                        }
                        a += 8;
                        if (c == 0xFF) {
                            r = 2;
                            continue;
                        }
                        v = 0;
                        int t = 0;
                        do {
                            v <<= 8;
                            v |= (getBits(sp, a, 8) & 0xFF);
                            a += 8;
                        } while (++t < 8);
                    }

                    if ((v & (1L << j)) != 0)
                        st[i] |= c << n * 8;
                    i++;
                    if (--j < 0) {
                        j = 63;
                        v = 0;
                        if (r > 0) {
                            r--;
                            i -= 64;
                        }
                    }
                } while (i < 64 * 4);
            } while (--n >= 0);

            if (st == SP0) {
                st = SP1;
                sp = sp1;
            } else
                break;
        }
    }

    static final void des(boolean encrypt, int[] key, byte[] iv,
                          byte[] data, int off, int len,
                          byte[] to, int pos) {
        final int[] sp0 = SP0, sp1 = SP1;
        int l, r, t, u, i, il = 0, ir = 0, al = 0, ar = 0, k, K = key.length;

        if (iv != null) {
            il = lsbf4(iv, 0);
            ir = lsbf4(iv, 4);
        }

        len += off;
        while (off < len) {
            l = lsbf4(data, off);
            r = lsbf4(data, off + 4);
            off += 8;

            if (iv != null) {
                if (encrypt) {
                    l ^= il;
                    r ^= ir;
                } else {
                    al = l;
                    ar = r;
                }
            }

            l ^= (t = (r >>> 4 ^ l) & 0x0f0f0f0f);
            r = (r ^= t << 4) ^ (t = (l >>> 16 ^ r) & 0x0000ffff);
            l = (l ^= t << 16) ^ (t = (r >>> 2 ^ l) & 0x33333333);
            r = (r ^= t << 2) ^ (t = (l >>> 8 ^ r) & 0x00ff00ff);
            l = (l ^= t << 8) ^ (t = (r >>> 1 ^ l) & 0x55555555);
            r ^= t << 1;

            u = r << 1 | r >>> 31;
            r = l << 1 | l >>> 31;

            k = 32;
            do {
                l = u;

                i = k - 32;
                do {
                    t = ((u = r ^ key[i + 1]) >>> 4 | u << 28) | 0xC040C040;
                    u = (r ^ key[i]) | 0x80008000;

                    l ^= sp0[t & 0x7f] |
                         sp0[t >>> 8 & 0xff] |
                         sp1[t >>> 16 & 0x7f] |
                         sp1[t >>> 24] |
                         sp0[u & 0x3f] |
                         sp0[u >>> 8 & 0xbf] |
                         sp1[u >>> 16 & 0x3f] |
                         sp1[u >>> 24 & 0xbf];

                    t = ((u = l ^ key[i + 3]) >>> 4 | u << 28) | 0xC040C040;
                    u = (l ^ key[i + 2]) | 0x80008000;

                    r ^= sp0[t & 0x7f] |
                         sp0[t >>> 8 & 0xff] |
                         sp1[t >>> 16 & 0x7f] |
                         sp1[t >>> 24] |
                         sp0[u & 0x3f] |
                         sp0[u >>> 8 & 0xbf] |
                         sp1[u >>> 16 & 0x3f] |
                         sp1[u >>> 24 & 0xbf];
                } while ((i += 4) < k);

                if ((k += 32) > K)
                    break;
                u = r;
                r = l;
            } while (true);

            l = l >>> 1 | l << 31;
            r = r >>> 1 | r << 31;

            l ^= (t = (r >>> 1 ^ l) & 0x55555555);
            r = (r ^= t << 1) ^ (t = (l >>> 8 ^ r) & 0x00ff00ff);
            l = (l ^= t << 8) ^ (t = (r >>> 2 ^ l) & 0x33333333);
            r = (r ^= t << 2) ^ (t = (l >>> 16 ^ r) & 0x0000ffff);
            l = (l ^= t << 16) ^ (t = (r >>> 4 ^ l) & 0x0f0f0f0f);
            r ^= t << 4;

            if (iv != null) {
                if (encrypt) {
                    il = l;
                    ir = r;
                } else {
                    l ^= il;
                    r ^= ir;
                    il = al;
                    ir = ar;
                }
            }

            if (to != null) {
                lsbf4(l, to, pos);
                lsbf4(r, to, pos + 4);
                pos += 8;
            }
        }

        if (iv != null) {
            lsbf4(il, iv, 0);
            lsbf4(ir, iv, 4);
        }
    }

    private static final HashMap bigIntegerMap = new HashMap();
    private static final HashMap bigIntegerInverseMap = new HashMap();
    protected static final HashMap rsaKeysMap = new HashMap();

    static final byte[] rsa(boolean padData, int padType, byte[][] key,
                            byte[] data, int off, int len) {
        CachingKey ck = new CachingKey(key, data, off, len, padData);
        CachingKey result = (CachingKey) rsaKeysMap.get(ck);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "size:" + rsaKeysMap.size());

        if (result != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "rsa.caching successful:" + result.hashcode);
            result.successfulUses += 1;
            result.reused = true;
            return result.result;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "rsa.caching failed:" + ck.hashcode);
            if (rsaKeysMap.size() > MAX_CACHE) {
                synchronized (rsaKeysMap) {
                    CachingKey[] keys = (CachingKey[]) rsaKeysMap.keySet().toArray(new CachingKey[rsaKeysMap.size()]);
                    Arrays.sort(keys, cachingKeyComparator);
                    if (cachingKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                        for (int i = 0; i < MAX_CACHE / 5; i++) {
                            rsaKeysMap.remove(keys[i]);
                            keys[i + 1 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 2 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 3 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 4 * MAX_CACHE / 5].successfulUses--;
                        }
                    } else {
                        for (int i = 0; i < MAX_CACHE / 5; i++) {
                            rsaKeysMap.remove(keys[keys.length - 1 - i]);
                            keys[keys.length - 1 - i - 1 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 2 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 3 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 4 * MAX_CACHE / 5].successfulUses--;
                        }
                    }
                }
            }
        }

        /** Used for both signing (padData == true) and verifying (padData == false) **/
        byte[] ret = null;

        int i, l, ml, mlb, kl;
        BigInteger[] k = new BigInteger[(kl = key.length)];

        if (kl == 8) {
            i = 3;
            l = kl;
            if (padType == 3)
                k[0] = new BigInteger(key[0]);
        } else {
            i = 0;
            l = 2;
        }
        do {
            k[i] = new BigInteger(key[i]);
        } while (++i < l);
        mlb = (i == 2) ? (k[0].bitLength()) : (k[3].bitLength() + k[4].bitLength());
        ml = (mlb + 7) / 8;

        byte[] buf;
        BigInteger b;

        if (padType != 3 || !padData && padType == 3) {
            System.arraycopy(data, off, (buf = new byte[ml + 1]), ml + 1 - len, len);

            if ((padData) && (padType != 0)) {
                l = ml - len - 3;
                Random rand = null;
                if ((buf[2] = (byte) padType) == 2)
                    rand = new Random(System.currentTimeMillis() ^ data.hashCode() ^ l);
                byte a = (byte) 0xFF;
                for (i = 3; l-- > 0;) {
                    if (padType == 2)
                        while ((a = (byte) rand.nextInt()) == 0);
                    buf[i++] = a;
                }
            }
            b = new BigInteger(buf);

        } else {
            if ((buf = padISO9796(data, off, len, mlb)) == null)
                return null;
            b = new BigInteger(1, buf);
        }

        if (kl > 3) {
            BigInteger m1 = b.remainder(k[3]).modPow(k[5], k[3]);
            BigInteger m2 = b.remainder(k[4]).modPow(k[6], k[4]);
            b = m1.add(k[3]).subtract(m2).multiply(k[7]).remainder(k[3]).multiply(k[4]).add(m2);
        } else
            b = b.modPow(k[1], k[0]);

        if (padType == 3) {
            if (!padData && !b.remainder(BigInteger.valueOf(16)).equals(BigInteger.valueOf(6)) ||
                padData && b.multiply(BigInteger.valueOf(2)).compareTo(k[0]) == 1)
                b = k[0].subtract(b);
        }

        buf = b.toByteArray();
        kl = 0;
        if (padData || padType == 3) {
            if ((i = buf.length - (l = ml)) == 0)
                return buf;
            if (i < 0) {
                kl = -i;
                i = 0;
            }
        } else if (padType == 0) {
            l = buf.length - (i = ((buf[0] == 0) && (buf.length > 1)) ? 1 : 0);
        } else if (buf[0] == padType) {
            for (i = 1; (i < buf.length) && (buf[i++] != 0););
            if ((l = buf.length - i) == 0)
                return null;
        } else
            return null;
        ret = new byte[l];
        System.arraycopy(buf, i, ret, kl, l - kl);

        synchronized (rsaKeysMap) {
            rsaKeysMap.put(ck, ck);
            ck.result = ret;
            ck.successfulUses = 0;
        }
        return ret;
    }

    static final byte[] signISO9796(byte[][] key, byte[] data, int off, int len) {
        return signISO9796(key, data, off, len, false);
    }

    static class CachingKey {

        boolean reused = false;
        long successfulUses;
        byte[][] key;
        byte[] data;
        int off;
        int len;
        boolean useJCE;

        int hashcode;

        byte[] result;

        public CachingKey(byte[][] key, byte[] data, int off, int len, boolean useJCE) {
            this.key = key;
            this.data = data;
            this.off = off;
            this.len = len;
            this.useJCE = useJCE;
            this.successfulUses = 0;
            this.reused = false;

            hashcode = 0;
            if (key != null && key.length > 0) {
                if (key[0] != null && key[0].length > 0) {
                    hashcode += key[0][0];
                }
            }
            if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    hashcode += data[i];
                }
            }

            hashcode += off + len;

            if (off != 0)
                hashcode *= off;

            if (useJCE) {
                hashcode *= 2;
            }
        }

        @Override
        public boolean equals(Object to) {
            if (!(to instanceof CachingKey)) {
                return false;
            }

            CachingKey ck = (CachingKey) to;

            if (hashcode != ck.hashcode) {
                return false;
            }

            if (len != ck.len) {
                return false;
            }

            if (key != null) {
                if (ck.key == null) {
                    return false;
                } else {
                    if (key.length != ck.key.length) {
                        return false;
                    }
                }
                for (int i = 0; i < key.length; i++) {
                    if (key[i] != null) {
                        if (ck.key[i] == null) {
                            return false;
                        } else {
                            if (key[i].length != ck.key[i].length) {
                                return false;
                            }
                        }
                        for (int o = 0; o < key[i].length; o++) {
                            if (key[i][o] != ck.key[i][o]) {
                                return false;
                            }
                        }
                    } else {
                        if (ck.key[i] != null) {
                            return false;
                        }
                    }
                }
            } else {
                if (ck.key != null) {
                    return false;
                }
            }

            if (data != null) {
                if (ck.data == null) {
                    return false;
                } else {
                    if (data.length != ck.data.length) {
                        return false;
                    }
                }
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != ck.data[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.data != null) {
                    return false;
                }
            }

            if (off != ck.off) {
                return false;
            }

            if (useJCE != ck.useJCE) {
                return false;
            }

            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return hashcode;
        }

    }

    protected static final HashMap cryptoKeysMap = new HashMap();

    static final byte[] signISO9796(byte[][] key, byte[] data, int off, int len, boolean useJCE) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "crypto.map.size=" + cryptoKeysMap.size());

        CachingKey ck = new CachingKey(key, data, off, len, useJCE);
        CachingKey result = (CachingKey) cryptoKeysMap.get(ck);

        if (result != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "crypto.caching successful:" + result.hashcode);
            result.successfulUses += 1;
            result.reused = true;
            return result.result;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "crypto.caching failed:" + ck.hashcode);
            if (cryptoKeysMap.size() > MAX_CACHE) {
                synchronized (cryptoKeysMap) {
                    CachingKey[] keys = (CachingKey[]) cryptoKeysMap.keySet().toArray(new CachingKey[cryptoKeysMap.size()]);
                    Arrays.sort(keys, cachingKeyComparator);
                    if (cachingKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                        for (int i = 0; i < MAX_CACHE / 5; i++) {
                            cryptoKeysMap.remove(keys[i]);
                            keys[i + 1 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 2 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 3 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 4 * MAX_CACHE / 5].successfulUses--;
                        }
                    } else {
                        for (int i = 0; i < MAX_CACHE / 5; i++) {
                            cryptoKeysMap.remove(keys[keys.length - 1 - i]);
                            keys[keys.length - 1 - i - 1 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 2 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 3 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 4 * MAX_CACHE / 5].successfulUses--;
                        }
                    }
                }
            }
        }

        byte[] sig = null;
        if (useJCE) {
            try {
                BigInteger n = new BigInteger(key[0]);
                BigInteger e = new BigInteger(key[2]);
                BigInteger p = new BigInteger(key[3]);
                BigInteger q = new BigInteger(key[4]);
                BigInteger d = e.modInverse((p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE)));
                KeyFactory kFact = null;
                kFact = KeyFactory.getInstance(CRYPTO_ALGORITHM);

                RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(n, d);
                PrivateKey privKey = kFact.generatePrivate(privKeySpec);
                Signature rsaSig = null;
                rsaSig = Signature.getInstance("SHA256withRSA");
                rsaSig.initSign(privKey);
                rsaSig.update(data, off, len);
                sig = rsaSig.sign();
            } catch (java.security.NoSuchAlgorithmException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1296");
            } catch (java.security.spec.InvalidKeySpecException e) {
                Tr.debug(tc, "Error: KeySpec invalid");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1304");
            } catch (java.security.InvalidKeyException e) {
                Tr.debug(tc, "Error: Key invalid");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1308");
            } catch (java.security.SignatureException e) {
                Tr.debug(tc, "Error: Signature operation failed");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCryptoCrypto", "1312");
            }

        } else {
            sig = rsa(true, 3, key, data, off, len);
        }

        synchronized (cryptoKeysMap) {
            cryptoKeysMap.put(ck, ck);
            ck.result = sig;
            ck.successfulUses = 0;
        }

        return sig;
    }

    static final boolean verifyISO9796(byte[][] key, byte[] data, int off, int len,
                                       byte[] sig, int sigOff, int sigLen) {
        return verifyISO9796(key, data, off, len, sig, sigOff, sigLen, false);
    }

    protected static final HashMap verifyKeysMap = new HashMap();

    static class CachingVerifyKey {

        boolean reused = false;
        long successfulUses;

        /*
         * byte[][] key, byte[] data, int off, int len,
         * byte[] sig, int sigOff, int sigLen, boolean useJCE)
         */

        byte[][] key;
        byte[] data;
        int off;
        int len;
        byte[] sig;
        int sigOff;
        int sigLen;
        boolean useJCE;

        int hashcode;

        boolean result;

        public CachingVerifyKey(byte[][] key, byte[] data, int off, int len, byte[] sig, int sigOff, int sigLen, boolean useJCE) {
            this.key = key;
            this.data = data;
            this.off = off;
            this.len = len;
            this.sig = sig;
            this.sigOff = sigOff;
            this.sigLen = sigLen;
            this.useJCE = useJCE;
            this.successfulUses = 0;
            this.reused = false;

            hashcode = 0;
            if (key != null && key.length > 0) {
                if (key[0] != null && key[0].length > 0) {
                    hashcode += key[0][0];
                }
            }
            if (data != null) {
                for (int i = 0; i < data.length && i < 10; i++) {
                    hashcode += data[i];
                }
                for (int i = data.length - 1; i >= 0 && i > data.length - 10; i--) {
                    hashcode += data[i];
                }
            }

            hashcode += off;

            if (off != 0)
                hashcode *= off;

            if (useJCE) {
                hashcode *= 2;
            }
        }

        @Override
        public boolean equals(Object to) {
            if (!(to instanceof CachingVerifyKey)) {
                return false;
            }

            CachingVerifyKey ck = (CachingVerifyKey) to;

            if (hashcode != ck.hashcode) {
                return false;
            }

            if (len != ck.len) {
                return false;
            }

            if (key != null) {
                if (ck.key == null) {
                    return false;
                } else {
                    if (key.length != ck.key.length) {
                        return false;
                    }
                }
                for (int i = 0; i < key.length; i++) {
                    if (key[i] != null) {
                        if (ck.key[i] == null) {
                            return false;
                        } else {
                            if (key[i].length != ck.key[i].length) {
                                return false;
                            }
                        }
                        for (int o = 0; o < key[i].length; o++) {
                            if (key[i][o] != ck.key[i][o]) {
                                return false;
                            }
                        }
                    } else {
                        if (ck.key[i] != null) {
                            return false;
                        }
                    }
                }
            } else {
                if (ck.key != null) {
                    return false;
                }
            }

            if (data != null) {
                if (ck.data == null) {
                    return false;
                } else {
                    if (data.length != ck.data.length) {
                        return false;
                    }
                }
                for (int i = 0; i < data.length; i++) {
                    if (data[i] != ck.data[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.data != null) {
                    return false;
                }
            }

            if (sig != null) {
                if (ck.sig == null) {
                    return false;
                } else {
                    if (sig.length != ck.sig.length) {
                        return false;
                    }
                }
                for (int i = 0; i < sig.length; i++) {
                    if (sig[i] != ck.sig[i]) {
                        return false;
                    }
                }
            } else {
                if (ck.sig != null) {
                    return false;
                }
            }

            if (off != ck.off) {
                return false;
            }

            if (useJCE != ck.useJCE) {
                return false;
            }

            if (sigOff != ck.sigOff) {
                return false;
            }

            if (sigLen != ck.sigLen) {
                return false;
            }

            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return hashcode;
        }

    }

    static final Comparator cachingVerifyKeyComparator = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            CachingVerifyKey k1 = (CachingVerifyKey) o1;
            CachingVerifyKey k2 = (CachingVerifyKey) o2;
            if (k1.successfulUses < k2.successfulUses) {
                return -1;
            } else if (k1.successfulUses == k2.successfulUses) {
                return 0;
            } else {
                return 1;
            }
        }
    };

    static final Comparator cachingKeyComparator = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            CachingKey k1 = (CachingKey) o1;
            CachingKey k2 = (CachingKey) o2;
            if (!k1.reused) {
                if (k2.reused) {
                    return -1;
                }
            } else {
                if (!k2.reused) {
                    return 1;
                }
            }
            if (k1.successfulUses < k2.successfulUses) {
                return -1;
            } else if (k1.successfulUses == k2.successfulUses) {
                return 0;
            } else {
                return 1;
            }
        }
    };

    static final int MAX_CACHE = 2000;

    static final boolean verifyISO9796(byte[][] key, byte[] data, int off, int len,
                                       byte[] sig, int sigOff, int sigLen, boolean useJCE) {

        CachingVerifyKey ck = new CachingVerifyKey(key, data, off, len, sig, sigOff, sigLen, useJCE);
        CachingVerifyKey result = (CachingVerifyKey) verifyKeysMap.get(ck);

        if (tc.isDebugEnabled())
            Tr.debug(tc, "v.size:" + verifyKeysMap.size());

        if (result != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "verify.caching successful:" + result.hashcode);
            result.successfulUses += 1;
            result.reused = true;
            return result.result;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "verify.caching failed:" + ck.hashcode);
            if (verifyKeysMap.size() > MAX_CACHE) {
                synchronized (verifyKeysMap) {
                    CachingVerifyKey[] keys = (CachingVerifyKey[]) verifyKeysMap.keySet().toArray(new CachingVerifyKey[verifyKeysMap.size()]);
                    Arrays.sort(keys, cachingVerifyKeyComparator);
                    if (cachingVerifyKeyComparator.compare(keys[0], keys[keys.length - 1]) < 0) {
                        for (int i = 0; i < MAX_CACHE / 5; i++) {
                            verifyKeysMap.remove(keys[i]);
                            keys[i + 1 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 2 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 3 * MAX_CACHE / 5].successfulUses--;
                            keys[i + 4 * MAX_CACHE / 5].successfulUses--;
                        }
                    } else {
                        for (int i = 0; i < MAX_CACHE / 5; i++) {
                            verifyKeysMap.remove(keys[keys.length - 1 - i]);
                            keys[keys.length - 1 - i - 1 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 2 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 3 * MAX_CACHE / 5].successfulUses--;
                            keys[keys.length - 1 - i - 4 * MAX_CACHE / 5].successfulUses--;
                        }
                    }
                }
            }
        }

        boolean verified = false;
        if (useJCE) {
            try {
                BigInteger n = new BigInteger(key[0]);
                BigInteger e = new BigInteger(key[1]);
                KeyFactory kFact = null;
                kFact = KeyFactory.getInstance(CRYPTO_ALGORITHM, "IBMJCE");
                RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(n, e);
                PublicKey pubKey = kFact.generatePublic(pubKeySpec);
                Signature rsaSig = null;
                rsaSig = Signature.getInstance("SHA256withRSA");
                rsaSig.initVerify(pubKey);
                rsaSig.update(data, off, len);
                verified = rsaSig.verify(sig);
            } catch (java.security.NoSuchAlgorithmException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1614");
            } catch (java.security.NoSuchProviderException e) {
                Tr.debug(tc, "Error: Provider not found");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1618");
            } catch (java.security.spec.InvalidKeySpecException e) {
                Tr.debug(tc, "Error: KeySpec invalid");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1622");
            } catch (java.security.InvalidKeyException e) {
                Tr.debug(tc, "Error: Key invalid");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1626");
            } catch (java.security.SignatureException e) {
                Tr.debug(tc, "Error: Signature operation failed");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1630");
            }

        } else {
            sig = rsa(false, 3, key, sig, sigOff, sigLen);
            data = padISO9796(data, off, len, new BigInteger(key[0]).bitLength());
            verified = data.length == sig.length && cmp(data, 0, sig, 0, sig.length);
        }
        synchronized (verifyKeysMap) {
            verifyKeysMap.put(ck, ck);
            ck.result = verified;
            ck.successfulUses = 0;
        }
        return verified;
    }

    static final byte[] padISO9796(byte[] data, int off, int len, int sigbits) {
        return padISO9796(data, off, len, sigbits, false);
    }

    static final byte[] padISO9796(byte[] data, int off, int len, int sigbits, boolean useJCE) {
        byte[] pad = null;
        if (useJCE) {
            // ISO9796 Padding is not supported by IBMJCEFIPS and is not FIPS approvable
        } else {
            sigbits--;
            if (len * 16 > sigbits + 3)
                return null;
            pad = new byte[(sigbits + 7) / 8];
            for (int i = 0; i < pad.length / 2; i++)
                pad[pad.length - 1 - 2 * i] = data[off + len - 1 - i % len];
            if ((pad.length & 1) != 0)
                pad[0] = data[off + len - 1 - (pad.length / 2) % len];
            long perm = 0x1CA76BD0F249853EL;;
            for (int i = 0; i < pad.length / 2; i++) {
                int j = pad.length - 1 - 2 * i;
                pad[j - 1] = (byte) ((((perm >> ((pad[j] >>> 2) & 0x3C)) & 0xF) << 4) | ((perm >> ((pad[j] & 0xf) << 2)) & 0xF));
            }
            pad[pad.length - 2 * len] ^= 1;
            int n = sigbits % 8;
            pad[0] &= (byte) ((1 << n) - 1);
            pad[0] |= 1 << (n - 1 + 8) % 8;
            pad[pad.length - 1] = (byte) (pad[pad.length - 1] << 4 | 0x06);
        }
        return pad;
    }

    static final void setRSAKey(byte[][] key) {
        BigInteger[] k = new BigInteger[8];
        for (int i = 0; i < 8; i++)
            if (key[i] != null)
                k[i] = new BigInteger(1, key[i]);

        if (k[3].compareTo(k[4]) < 0) {
            BigInteger tmp;
            tmp = k[3];
            k[3] = k[4];
            k[4] = tmp;
            tmp = k[5];
            k[5] = k[6];
            k[6] = tmp;
            k[7] = null;
        }
        if (k[7] == null)
            k[7] = k[4].modInverse(k[3]);
        if (k[0] == null)
            k[0] = k[3].multiply(k[4]);
        if (k[1] == null)

            k[1] = k[2].modInverse(k[3].subtract(BigInteger.valueOf(1)).multiply(k[4].subtract(BigInteger.valueOf(1))));
        if (k[5] == null)
            k[5] = k[1].remainder(k[3].subtract(BigInteger.valueOf(1)));
        if (k[6] == null)
            k[6] = k[1].remainder(k[4].subtract(BigInteger.valueOf(1)));

        for (int i = 0; i < 8; i++)
            key[i] = k[i].toByteArray();
    }

    static final byte[][] rsaKey(int len, boolean crt, boolean f4) {
        return rsaKey(len, crt, f4, false);
    }

    static final byte[][] rsaKey(int len, boolean crt, boolean f4, boolean useJCE) {
        byte[][] key = new byte[crt ? 8 : 3][];
        if (useJCE) {
            BigInteger pub_e = BigInteger.valueOf(f4 ? 0x10001 : 3);
            KeyPair pair = null;
            try {
                RSAKeyGenParameterSpec rsaKeyGenSpec = new RSAKeyGenParameterSpec(len * 8, pub_e);
                KeyPairGenerator keyGen = null;
                SecureRandom random = null;
                keyGen = KeyPairGenerator.getInstance(CRYPTO_ALGORITHM, "IBMJCE");
                random = SecureRandom.getInstance("IBMSecureRandom");

                // keyGen.initialize(rsaKeyGenSpec, random);   // cyc - cause java.security.InvalidAlgorithmParameterException: Parameters not supported
                keyGen.initialize(len * 8, random);
                pair = keyGen.generateKeyPair();
                RSAPublicKey rsaPubKey = (RSAPublicKey) pair.getPublic();
                byte[] publicKey = rsaPubKey.getEncoded();
                RSAPrivateCrtKey rsaPrivKey = (RSAPrivateCrtKey) pair.getPrivate();
                byte[] privateKey = rsaPrivKey.getEncoded();

                BigInteger e = rsaPubKey.getPublicExponent();
                BigInteger n = rsaPubKey.getModulus();
                BigInteger pe = rsaPrivKey.getPrivateExponent();
                key[0] = n.toByteArray();
                key[1] = crt ? null : pe.toByteArray();
                key[2] = e.toByteArray();
                if (crt) {
                    BigInteger p = rsaPrivKey.getPrimeP();
                    BigInteger q = rsaPrivKey.getPrimeQ();
                    BigInteger ep = rsaPrivKey.getPrimeExponentP();
                    BigInteger eq = rsaPrivKey.getPrimeExponentQ();
                    BigInteger c = rsaPrivKey.getCrtCoefficient();
                    key[3] = p.toByteArray();
                    key[4] = q.toByteArray();
                    key[5] = ep.toByteArray();
                    key[6] = eq.toByteArray();
                    key[7] = c.toByteArray();

                }
            } catch (java.security.NoSuchAlgorithmException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "1796");
            } catch (java.security.NoSuchProviderException e) {
                Tr.debug(tc, "Error: Provider not found");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.ltpa.LTPACrypto", "1800");
            }
        } else {
            BigInteger p, q, n, d;
            BigInteger e = BigInteger.valueOf(f4 ? 0x10001 : 3);
            BigInteger one = BigInteger.valueOf(1), two = BigInteger.valueOf(2);
            byte[] b = new byte[(len /= 2) + 1];

            for (p = null;;) {
                for (q = null;;) {
                    if (q == null) {
                        random(b, 1, len);
                        b[1] |= 0xC0;
                        b[len] |= 1;
                        q = new BigInteger(b);
                    } else {
                        q = q.add(two);
                        if (q.bitLength() > len * 8) {
                            q = null;
                            continue;
                        }
                    }

                    if (q.isProbablePrime(32) && e.gcd(q.subtract(one)).equals(one))
                        break;
                }

                if (p == null)
                    p = q;
                else {
                    n = p.multiply(q);
                    if (n.bitLength() == len * 2 * 8) {

                        d = e.modInverse((p.subtract(one)).multiply(q.subtract(one)));

                        if (((p.modPow(e, n)).modPow(d, n)).equals(p))
                            break;
                    }
                    p = null;
                }
            }

            key[0] = n.toByteArray(); // modulus
            key[1] = crt ? null : d.toByteArray(); //private exponient if a CRT key
            key[2] = e.toByteArray(); //public exponient

            if (crt) {
                if (p.compareTo(q) < 0) {
                    e = p;
                    p = q;
                    q = e;
                }
                key[3] = p.toByteArray(); //PrimeP
                key[4] = q.toByteArray(); //PrimeQ
                key[5] = d.remainder(p.subtract(one)).toByteArray(); //PrimeExponentP   \
                key[6] = d.remainder(q.subtract(one)).toByteArray(); //PrimeExponentQ    - looks like JCE sets these to zero. You could calculate these if you want to.
                key[7] = q.modInverse(p).toByteArray(); //getCrtCoefficient /

            }
        }
        return key;
    }

    static final boolean dsa(int mode, byte[][] key, byte[] data, int off, int len,
                             byte[] sig, int pos) {
        int i = 0, j, l;
        BigInteger[] p = new BigInteger[6];
        do {
            p[i] = new BigInteger(key[i]);
        } while (++i < 4);
        if (p[1].bitLength() != 160)
            return false;

        byte[] b = new byte[21];
        if (mode == 2)
            sha(null, data, off, len, b, 1);
        else if (len == 20)
            System.arraycopy(data, off, b, 1, 20);
        else
            return false;

        BigInteger hash = new BigInteger(b);

        if (mode == 0) {
            b = new byte[20];
            random(b, 0, 20);
            b[0] &= (byte) 0x7F;

            BigInteger k = new BigInteger(b);

            p[4] = p[2].modPow(k, p[0]).remainder(p[1]);

            p[5] = (k.modInverse(p[1])).multiply(p[3].multiply(p[4]).add(hash)).remainder(p[1]);

            i = 0;
            do {
                sig[pos + i] = 0;
            } while (++i < 40);

            i = 0;
            do {
                b = p[i + 4].toByteArray();
                j = ((l = b.length) > 20) ? 1 : 0;
                l -= j;
                System.arraycopy(b, j, sig, pos + (20 * i) + (20 - l), l);
            } while (++i < 2);
            return true;
        }

        i = 0;
        do {
            if (mode == 1)
                System.arraycopy(sig, pos + (i * 20), b, 1, 20);
            else if ((sig[pos] == 2) && ((j = sig[pos + 1]) > 0) &&
                     (j <= 21)) {
                b = new byte[j];
                System.arraycopy(sig, pos + 2, b, 0, j);
                pos += j + 2;
            } else
                return false;
            p[i + 4] = new BigInteger(b);
        } while (++i < 2);

        BigInteger w = p[5].modInverse(p[1]);

        BigInteger u1 = hash.multiply(w).remainder(p[1]);

        BigInteger u2 = p[4].multiply(w).remainder(p[1]);

        BigInteger v = (p[2].modPow(u1, p[0])).multiply(p[3].modPow(u2, p[0])).remainder(p[0]).remainder(p[1]);

        return v.equals(p[4]);
    }

    static final byte getBits(String s, int pos, int len) {
        int i, j;
        byte a = (byte) (s.charAt((i = pos / 7)));
        a &= 0x7F >>> (j = pos % 7);
        if ((j = len - (7 - j)) > 0)
            return (byte) ((a << j) | (byte) (s.charAt((i + 1))) >>> (7 - j));
        if (j < 0)
            return (byte) (a >>> -j);
        return a;
    }

    static final void printBytes(String name, byte[] b, int off, int len) {
        System.out.println(name);
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; off++) {
            buf.append("0123456789ABCDEF".charAt((b[off] >> 4) & 0xF));
            buf.append("0123456789ABCDEF".charAt(b[off] & 0xF));
            buf.append(" ");
            if (((++i) % 16 == 0) || (i == len)) {
                System.out.println(buf.toString());
                buf.setLength(0);
            }
        }
    }

    static final void printInfo(String label, int N, long t1, long t2, long t3, long t4) {
        System.out.print((label + "....................").substring(0, 20));
        for (int i = 0; i < 2; i++) {
            long tb = (i == 0 ? t1 : t3);
            long te = (i == 0 ? t2 : t4);
            if (tb == -1)
                System.out.print("      ");
            else {
                String s = "     " + (N * 1000 / (te - tb));
                System.out.print(s.substring(s.length() - 6));
            }
        }
        System.out.println();
    }

    static final boolean verifyBuf(byte[] b, int N, String alg) {
        for (int i = 0; i < 1024 * N; i++)
            if (b[i] != (byte) (i % 128)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, alg + " failed!");
                return true;
            }
        return false;
    }

    static final boolean testRSAKeys(byte[] b, byte[][] privKey, byte[][] pubKey) {
        int l = pubKey[0].length;
        if (pubKey[0][0] == 0)
            l--;
        long t1 = System.currentTimeMillis();
        byte[] rsa_b = rsa(true, 0, pubKey, b, 1, l);
        long t2 = System.currentTimeMillis();
        rsa_b = rsa(false, 0, privKey, rsa_b, 0, rsa_b.length);
        long t3 = System.currentTimeMillis();

        String s = ("RSA/" + (privKey.length == 8 ? "CRT/" : "") + (l * 8) +
                    (pubKey[1].length == 1 && pubKey[1][0] == 3 ? "/3" : "/F4") +
                    " ...................");
        s = s.substring(0, 20);
        for (int i = 0; i < 2; i++) {
            String ss = "     " + (i == 0 ? (t3 - t2) : (t2 - t1));
            s += ss.substring(ss.length() - 5);
        }
        for (int i = 0; i < rsa_b.length; i++)
            if (rsa_b[i] != (byte) ((i + 1) % 128)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "RSA failed!");
                return true;
            }
        return false;
    }

    static final boolean testDSAKeys(byte[] b, byte[][] privKey, byte[][] pubKey) {
        int l = privKey[0].length;
        if (privKey[0][0] == 0)
            l--;
        long t1 = System.currentTimeMillis();
        byte[] sig = new byte[40];
        dsa(0, privKey, b, 0, 20, sig, 0);
        long t2 = System.currentTimeMillis();
        boolean ok = dsa(1, pubKey, b, 0, 20, sig, 0);
        long t3 = System.currentTimeMillis();

        String s = ("DSA/" + (l * 8) + " ...................").substring(0, 20);
        for (int i = 0; i < 2; i++) {
            String ss = "     " + (i == 0 ? (t2 - t1) : (t3 - t2));
            s += ss.substring(ss.length() - 5);
        }
        if (!ok)
            if (tc.isDebugEnabled())
                Tr.debug(tc, "DSA failed!");
        return !ok;
    }

    static String[][] rsaKeyMaterial = {

                                         { "4svq2jqtxo3zn2njenso9vwyg2bynvo08ekktj4d7sqwk9s3oz",
                                           "4se994le3trmoep5f74ytxfupr2o0oi9dem4nzailb4k4g5e7j",
                                           "1ekh" },

                                         { "uk5febz1u9c5x7knn185refnb02syox36xqwae0lm30z9j9p03" +
                                           "hyu175dyxbiczds3k1n6jiwqdeyetwgsy1qrvje8a7o40cmb5",
                                           "ujsuw3e4k53dtzgbsm3tjpytf5h25i71r8cs8ijbigo607ceo5" +
                                                                                                "zy5toem0kp4oeb77tt86h7gkix5fjdq13sa7puya61b2ep82n",
                                           "3" }
    };

    static String[][] dsaKeyMaterial = {

                                         { "otj4bi3e6pxy54h5tkjwpuzycvm3ta6jg9f6lj52mvygb9l72y1tkrs0ppuldns6kem6vzw3fbwhinhdhpqjvn284fc0dsaz39h",
                                           "jpdh5mk2p667os7al4gmvbdfmar3bsv",
                                           "cdybrmm4x665tomdaiedafq3d2wiajhlkbeql7iui72eeayleaa3ppn7lhfdbrh508kum7havwgb7otsnme3pc8r7kipf55hvio",
                                           "lpb2xrb2yivmklm6i6pyzvagsu9qhdz",
                                           "6d3ng23juhszoxet3kkzw2ei7y3hxo67c9oqvuf5d1dpev7qzwhzy11tcaikknfxtr62zyk96d9vvhli6zw2b2sxbrnlc3xkuzy" },

                                         { "10uj5jh4khn7t93eh41c1d7sfptfuqiycpiimudbj62leu8fwnnt3k5cdkzynrvbhlflm3qe6sfwsjs3bbvjm8j8ctzaljlothj" +
                                           "tbujclhafng31uzf4zmj11qjni0z9ou77rap19wl7ps7v52fbuoycrgu6xohwoobiwfanlkh4t18wtw3kf1nsdxz7mwpu9ddu4cz",
                                           "s6zmy3zi8dumvm43ofheresn52f9trj",
                                           "z4asx4yhsha3vd0d0uhhnahzmtj1qg572k3frvtq46x9lrawlm4x70oc99d4qsplci9e8qjtaqt3sqf719tfojrwjnonkqbxm9o" +
                                                                              "p3ck61fcxx2q6l4vg1rizk9kn74pi9859nqqctvn9174smwqzosvdrnd89eykgocc09ph343gpen9lgo0h6dk32a35gut5wb6w1",
                                           "f8xedoxwqju60mngerxyt5jv7rl8wbg",
                                           "egc8c7ptmx0hr5i4x2bzgeumx8kcmc9jokca88r8e4k1ih802bnz9flr08topo1v7kodqg9yab3xpf2j0lv9zmg8jhh38okgjfe" +
                                                                              "ou1fb7xn6blo4t1m8fb64p849eaqa66f1c0ar7m1uwdwc9k57vr58frxezjd1w4sc4zp8s6wn89lmbzem0brt6phtukhg2qfgrn" }
    };

    static byte[][][] rsaKeys;
    static byte[][][] dsaKeys;

    // generate secret key may go away; generate3DESKey be used instead
    static final byte[] generateSecretKey(int length) {
        return new SecureRandom().getSeed(length);
    }

    static final byte[] generate3DESKey() {
        return generate3DESKey(false);
    }

    static final byte[] generate3DESKey(boolean useJCE) {
        byte[] rndSeed = null;
        if (useJCE) {
            try {
                // Here, we have a hard dependency on IBMJCE and IBMJCEFIPS provider
                SecureRandom random = null;
                KeyGenerator keyGen = null;
                random = SecureRandom.getInstance("IBMSecureRandom");
                keyGen = KeyGenerator.getInstance(ENCRYPT_ALGORITHM);
                keyGen.init(random);
                SecretKey key = keyGen.generateKey();
                rndSeed = key.getEncoded();
            } catch (java.security.NoSuchAlgorithmException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2157");
            }
        } else {
            int len = 24; //3DES
            rndSeed = new byte[len];
            random(rndSeed, 0, len);
        }
        return rndSeed;
    }

    static final byte[] encrypt(byte[] data, byte[] key) {
        return encrypt(data, key, CIPHER, true);
    }

    static final byte[] encrypt(byte[] data, byte[] key, String cipher, boolean useJCE) {
        // determine key length from the key which specifies whether it is 3DES or DES
        long start_time = 0;

        if (tc.isDebugEnabled()) {
            start_time = System.currentTimeMillis();
            Tr.debug(tc, "Cipher used to encrypt: " + cipher);
            Tr.debug(tc, "Data size: " + data.length);
            Tr.debug(tc, "Key size: " + key.length);
        }

        byte[] mesg = null;
        if (useJCE) {
            try {
                if (null == data) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Array was null");
                    return null;
                }

                SecretKey sKey = null;

                if (cipher.indexOf("AES") != -1) {
                    // 16 bytes = 128 bit key
                    sKey = new SecretKeySpec(key, 0, 16, "AES");
                } else {
                    DESedeKeySpec kSpec = new DESedeKeySpec(key);
                    SecretKeyFactory kFact = null;
                    kFact = SecretKeyFactory.getInstance(ENCRYPT_ALGORITHM);
                    sKey = kFact.generateSecret(kSpec);
                }

                Cipher ci = null;
                ci = Cipher.getInstance(cipher);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The Provider Cipher used to encrypt: " + ci.getProvider());
                    Tr.debug(tc, "The Algorithm Cipher used to encrypt: " + ci.getAlgorithm());
                }
                if (cipher.indexOf("ECB") == -1) {
                    if (cipher.indexOf("AES") != -1) {
                        if (ivs16 == null) {
                            setIVS16(key);
                        }
                        ci.init(Cipher.ENCRYPT_MODE, sKey, ivs16);
                    } else {
                        if (ivs8 == null) {
                            setIVS8(key);
                        }
                        ci.init(Cipher.ENCRYPT_MODE, sKey, ivs8);
                    }
                } else {
                    ci.init(Cipher.ENCRYPT_MODE, sKey);
                }

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "encrypt() Cipher.doFinal()\n   data: " + new String(data));
                mesg = ci.doFinal(data);

            } catch (java.security.NoSuchAlgorithmException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2256");
            } catch (java.security.InvalidKeyException e) {
                Tr.debug(tc, "Error: Key invalid");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2264");
            } catch (java.security.spec.InvalidKeySpecException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2267");
            } catch (javax.crypto.NoSuchPaddingException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2270");
            } catch (javax.crypto.IllegalBlockSizeException e) {
                // we get this exception when validating other token types
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2273");
            } catch (javax.crypto.BadPaddingException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2276");
            } catch (java.security.InvalidAlgorithmParameterException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2279");
            }

        } else {
            int len = key.length;
            int[] encrInternalKey = desKey(true, key, 0, len);

            // REMINDER: pad message to be multiple of 8
            mesg = padPKCS5(data);
            if (null == mesg) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Array was null");
                return null;
            }
            des(true, encrInternalKey, null, mesg, 0, mesg.length, mesg, 0);
        }

        if (tc.isDebugEnabled()) {
            long end_time = System.currentTimeMillis();
            Tr.debug(tc, "Total encryption time: " + (end_time - start_time));
        }

        return mesg;
    }

    static final byte[] decrypt(byte[] mesg, byte[] key) {
        return decrypt(mesg, key, CIPHER, true);
    }

    static final byte[] decrypt(byte[] mesg, byte[] key, String cipher, boolean useJCE) {

        long start_time = 0;

        if (tc.isDebugEnabled()) {
            start_time = System.currentTimeMillis();
            Tr.debug(tc, "Cipher used to decrypt: " + cipher);
            Tr.debug(tc, "key size: " + key.length);
        }

        byte[] tmpMesg = null;
        if (useJCE) {
            try {
                if (null == mesg) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Array was null");
                    return null;
                }

                SecretKey sKey = null;

                if (cipher.indexOf("AES") != -1) {
                    // 16 bytes = 128 bit key
                    sKey = new SecretKeySpec(key, 0, 16, "AES");
                } else {
                    DESedeKeySpec kSpec = new DESedeKeySpec(key);
                    SecretKeyFactory kFact = null;
                    kFact = SecretKeyFactory.getInstance(ENCRYPT_ALGORITHM);
                    sKey = kFact.generateSecret(kSpec);
                }

                Cipher ci = null;
                ci = Cipher.getInstance(cipher);

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The Provider Cipher used to decrypt: " + ci.getProvider());
                    Tr.debug(tc, "The Algorithm Cipher used to decrypt: " + ci.getAlgorithm());
                }

                if (cipher.indexOf("ECB") == -1) {
                    if (cipher.indexOf("AES") != -1) {

                        if (ivs16 == null) {
                            setIVS16(key);
                        }
                        ci.init(Cipher.DECRYPT_MODE, sKey, ivs16);
                    } else {

                        if (ivs8 == null) {
                            setIVS8(key);
                        }
                        ci.init(Cipher.DECRYPT_MODE, sKey, ivs8);
                    }
                } else {
                    ci.init(Cipher.DECRYPT_MODE, sKey);
                }

                tmpMesg = ci.doFinal(mesg);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "decrypt() Cipher.doFinal()\n   tmpMesg: " + new String(tmpMesg));

            } catch (java.security.NoSuchAlgorithmException e) {
                Tr.error(tc, "no such algorithm exception", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2385");
            } catch (java.security.InvalidKeyException e) {
                Tr.debug(tc, "Error: Key invalid");
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2393");
            } catch (java.security.spec.InvalidKeySpecException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2396");
            } catch (javax.crypto.NoSuchPaddingException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2399");
            } catch (javax.crypto.IllegalBlockSizeException e) {
                // we get this exception when validating other token types
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2402");
            } catch (javax.crypto.BadPaddingException e) {
                Tr.debug(tc, "BadPaddingException validating token, normal when token generated from other factory.", new Object[] { e.getMessage() });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.audit.AuditCrypto", "2405");
            } catch (java.security.InvalidAlgorithmParameterException e) {
                Tr.error(tc, "security.ltpa.noalgorithm", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.auditAuditCrypto", "2408");
            }
        } else {
            int len = key.length;
            int[] decrInternalKey = desKey(false, key, 0, len);

            // REMINDER: pad message to be multiple of 8

            des(false, decrInternalKey, null, mesg, 0, mesg.length, mesg, 0);
            tmpMesg = unpadPKCS5(mesg);
            if (null == tmpMesg) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Array was not properly paded");
                return null;
            }
        }

        if (tc.isDebugEnabled()) {
            long end_time = System.currentTimeMillis();
            Tr.debug(tc, "Total decryption time: " + (end_time - start_time));
        }

        return tmpMesg;
    }

    /**
     * PKCS #5 padding routines. PKCS #5 pads data blocks with 1 to 8 bytes
     * of data. The pad character is is tha same as the number of pad bytes.
     */

    /**
     * Accept a PKCS #5 padded array and return an unpadded byte array.
     *
     * @param aB a byte array containing the PKCS #5 padded data.
     * @return an unpadded byte array or <i>null</i> if the input array
     *         was not properly padded.
     */
    private static final byte[] unpadPKCS5(byte aB[]) {
        if (null == aB)
            return null;
        if (0 == aB.length)
            return null;

        int aBl = aB.length;

        int pc = aB[aBl - 1];

        if ((pc > 8) || (pc < 1))
            return null;

        for (int i = aBl - pc; i < aBl; i++) {
            if (aB[i] != pc)
                return null;
        }

        byte[] unpadB = new byte[aBl - pc];
        System.arraycopy(aB, 0, unpadB, 0, aBl - pc);
        return unpadB;
    }

    /**
     * Pad the specified byte array according to PKCS #5 and return a new (padded)
     * byte array.
     *
     * @param aB any byte array
     * @return a new, PKCS #5 padded byte array.
     */
    private static final byte[] padPKCS5(byte aB[]) {
        if (null == aB)
            return null;
        int aBl = aB.length;
        int pc = 8 - (aBl % 8);
        byte[] padB = new byte[aBl + pc];

        System.arraycopy(aB, 0, padB, 0, aBl);
        for (int i = aBl; i < padB.length; i++)
            padB[i] = (byte) pc;
        return padB;
    }

    /**
     * Convert a byte[] to a hexadecimal string
     **/
    public static String toHexString(byte[] bytes) {
        String hexString = null;
        for (int i = 0; i < bytes.length; i++) {
            String byteString = java.lang.Integer.toHexString(bytes[i]);
            if (byteString.length() == 1) {
                byteString = "0" + byteString;
            }
            hexString = hexString + byteString;
        }
        return hexString;
    }

    /**
     * Get 8 byte initialization vector
     **/
    public static IvParameterSpec getIVS8() {
        return ivs8;
    }

    /**
     * Get 16 byte initialization vector
     **/
    public static IvParameterSpec getIVS16() {
        return ivs16;
    }

    /**
     * Set 8 byte initialization vector
     **/
    public static synchronized void setIVS8(byte[] key) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setIVS8");

        if (ivs8 == null) // only set it once
        {
            try {
                byte[] iv8 = new byte[8];
                for (int i = 0; i < 8; i++) {
                    iv8[i] = key[i];
                }
                ivs8 = new IvParameterSpec(iv8);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setIVS8: ivs8 successfully set");
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setIVS8 unxepected exception setting initialization vector", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.ltpa.LTPAToken2Factory.initialize", "2539");
            }
        }
    }

    /**
     * Set 16 byte initialization vector
     **/
    public static synchronized void setIVS16(byte[] key) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setIVS16");

        if (ivs16 == null) // only set it once
        {
            try {
                byte[] iv16 = new byte[16];
                for (int i = 0; i < 16; i++) {
                    iv16[i] = key[i];
                }
                ivs16 = new IvParameterSpec(iv16);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setIVS16: ivs16 successfully set");
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "setIVS16 unxepected exception setting initialization vector", new Object[] { e });
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.security.ltpa.LTPAToken2Factory.initialize", "2568");
            }
        }
    }
}