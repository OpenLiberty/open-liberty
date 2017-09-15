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
package com.ibm.ws.http.channel.h2internal.frames;

public class LongEncoder {

    public byte[] encode(long Input, int N) {

        byte result[] = null;
        byte nMinus1 = (byte) (ipow(2, N) - 1);
        EncoderParms ep = null;

        if (Input < nMinus1) {
            result = new byte[1];
            result[0] = (byte) Input;
            return result;
        }

        else {
            ep = new EncoderParms();
            ep.value = Input - nMinus1;
            ep.index = 1;
            nextLevel(ep);
            result = ep.results;
            result[0] = (nMinus1);
            return result;
        }

    }

    public void nextLevel(EncoderParms epX) {

        byte levelValue = (byte) (epX.value & 0x7F); // save off current least signifcant 7 bits
        epX.value = epX.value >>> 7; // right shift, fill 0, to divide by 128
        //System.out.println("levelValue: " + levelValue + " epX.value: " + epX.value + " epX.index: " + epX.index);

        if (epX.value > 0) {
            epX.index++;

            nextLevel(epX);
            // Unwind from recursive call
            // Once we return for nextLevel, it means that the next MSB can now be store at the biggest index, then count down, for the next one
            // not last array index, so byte starts with a 1
            epX.results[epX.index] = (byte) ((byte) 0x80 | levelValue);
            epX.index--;

            return;
        } else {
            // first recursive end point we should return from

            // found the MSB, so size the array, and store at the large index of the array
            epX.results = new byte[epX.index + 1];

            epX.results[epX.index] = (levelValue);
            epX.index--;

            return;
        }
    }

    int ipow(int base, int exp) {
        int result = 1;
        while (exp != 0) {
            if ((exp & 1) != 0)
                result *= base;
            exp >>= 1;
            base *= base;
        }

        return result;
    }

}
