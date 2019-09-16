package com.ibm.ws.testing.opentracing.test;

import java.io.IOException;
import java.io.Reader;

/**
 * <p>Sub-string reader.  Used to create a reader on a sub-range of
 * a string.</p>
 *
 * <p>This class is not currently used, and is retained for future use.</p>
 */
public class SubStringReader extends Reader {

    public SubStringReader(String value) {
        this(value, 0, value.length());
    }

    public SubStringReader(String value, int initialSourceOffset, int finalSourceOffset) {
        if ( value == null ) {
            throw new IllegalArgumentException("The value is null.");
        }

        // Post:
        // (value != 0)

        if ( initialSourceOffset < 0 ) {
            throw new IndexOutOfBoundsException("The initial offset [ " + Integer.toString(initialSourceOffset) + " ] is less than [ 0 ]");
        } else if ( initialSourceOffset > finalSourceOffset ) {
            throw new IndexOutOfBoundsException("The initial offset [ " + Integer.toString(initialSourceOffset) + " ] is greater than the final offset [ " + Integer.toString(finalSourceOffset) + " ]");
        }
        int valueLength = value.length();
        if ( finalSourceOffset > valueLength ) {
            throw new IndexOutOfBoundsException("The final offset [ " + Integer.toString(initialSourceOffset) + " ] is greater than the value length [ " + Integer.toString(valueLength) + " ]");
        }

        // Post:
        // (initialOffset >= 0) && (initialOffset <= finalOffset)
        // (finalOffset <= value.length)

        this.source = value;
        this.isOpen = true;

        this.initialSourceOffset = initialSourceOffset;
        this.finalSourceOffset = finalSourceOffset;
        this.remainingSourceChars = finalSourceOffset - initialSourceOffset;

        this.nextSourceOffset = initialSourceOffset;
        this.markSourceOffset = initialSourceOffset;
    }

    //

    private final String source;
    private boolean isOpen;

    private final int initialSourceOffset;
    private final int finalSourceOffset;

    private int nextSourceOffset;
    private int remainingSourceChars;

    private int markSourceOffset;

    //

    @Override
    public boolean ready() {
        return isOpen;
    }

    @Override
    public void close() {
        if ( isOpen ) {
            isOpen = false;
        }
    }

    private void ensureOpen() throws IOException {
        if ( !isOpen ) {
            throw new IOException("Closed");
        }
    }

    //

    @Override
    public int read() throws IOException {
        ensureOpen(); // throws IOException

        if ( nextSourceOffset >= finalSourceOffset ) {
            return -1;

        } else {
            int nextChar = source.charAt(nextSourceOffset++);
            remainingSourceChars -= 1;
            return nextChar;
        }
    }

    @Override
    public int read(char targetBuffer[], int targetBufferOffset, int readCount) throws IOException {
        ensureOpen(); // throws IOException

        int targetLength = targetBuffer.length;
        if ( (targetBufferOffset < 0) || (targetBufferOffset > targetLength) ||
             (readCount < 0) || ((targetBufferOffset + readCount) > targetLength) ) {
            throw new IndexOutOfBoundsException(
                "Offset [ " + Integer.toString(targetBufferOffset) + " ]" +
                " and count [ " + Integer.toString(readCount) + " ]" +
                " not valid for length [ " + Integer.toString(targetLength) + " ]");
        }

        if ( readCount == 0 ) {
            return 0; // Trivial read.  Return immediately.
        }

        // if ( nextSourceOffset >= finalSourceOffset ) {
        if ( remainingSourceChars <= 0 ) {
            return -1; // No source characters are left.
        }

        if ( readCount > remainingSourceChars ) {
            readCount = remainingSourceChars; // Truncate to what is available.
        }

        source.getChars(
            nextSourceOffset, (nextSourceOffset + readCount),
            targetBuffer, targetBufferOffset);

        nextSourceOffset += readCount;
        remainingSourceChars -= readCount;

        return readCount;
    }

    @Override
    public long skip(long skipCount) throws IOException {
        ensureOpen(); // throws IOException

        if ( skipCount == 0 ) {
            // Trivial skip; do nothing.

        } if ( skipCount > 0 ) {
            if ( skipCount >= remainingSourceChars ) {
                skipCount = remainingSourceChars;
                nextSourceOffset = finalSourceOffset;
                remainingSourceChars = 0;
            } else {
                // Safe conversion: (skipCount > 0) && (skipCount < remainingValueChars)
                int intSkipCount = (int) skipCount;
                nextSourceOffset += intSkipCount;
                remainingSourceChars -= intSkipCount;
            }

        } else { // ( skipCount < 0 ) {
            int backupChars = initialSourceOffset - nextSourceOffset; // This is at most 0!
            if ( skipCount < backupChars) {
                skipCount = backupChars;
                nextSourceOffset = initialSourceOffset;
                remainingSourceChars = finalSourceOffset - initialSourceOffset;
            } else {
                // Safe conversion: (skipCount < 0) && (skipCount > backupChars)
                int intSkipCount = (int) skipCount;
                nextSourceOffset += intSkipCount; // Note: (intSkipCount < 0)
                remainingSourceChars -= intSkipCount; // Note: (intSkipCount < 0)
            }
        }

        return skipCount;
    }

    //

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        if ( readAheadLimit < 0 ){
            throw new IllegalArgumentException("Read-ahead limit [ " + Integer.toString(readAheadLimit) + " ] is less than [ 0 ].");
        }

        ensureOpen(); // throws IOException

        markSourceOffset = nextSourceOffset;
    }

    @Override
    public void reset() throws IOException {
        ensureOpen(); // throws IOException

        nextSourceOffset = markSourceOffset;
        remainingSourceChars = finalSourceOffset - nextSourceOffset;
    }
}