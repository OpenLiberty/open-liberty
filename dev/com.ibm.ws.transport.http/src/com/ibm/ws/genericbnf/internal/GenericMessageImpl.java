/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.genericbnf.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;

/**
 * A generic message class that adds abstract first line handling along
 * with the BNFHeaders manipulation. Subclasses will extend this class
 * and add their specific handling for the first line tokens.
 */
public abstract class GenericMessageImpl extends BNFHeadersImpl {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(GenericMessageImpl.class,
                                                         GenericConstants.GENERIC_TRACE_NAME,
                                                         null);

    /** Serialization ID */
    private static final long serialVersionUID = 2516122741254647791L;

    /** Flag on whether the first line is completely parsed */
    private transient boolean completedFirstLine = false;
    /** number of tokens read in the first line */
    private transient int numFirstLineTokensRead = 0;
    /** Flag on whether the first line has changed since being parsed */
    private transient boolean firstLineChanged = false;
    //PI34161 - Record the start time from the message parse
    public long startTime = 0;

    /**
     * Constructor of this generic message, called by subclass constructors.
     */
    protected GenericMessageImpl() {
        super();
    }

    /**
     * Called when the first token of the first line has been parsed.
     * 
     * @param token
     * @throws Exception
     */
    protected abstract void setParsedFirstToken(byte[] token) throws Exception;

    /**
     * Called when the second token of the first line has been parsed.
     * 
     * @param token
     * @throws Exception
     */
    protected abstract void setParsedSecondToken(byte[] token) throws Exception;

    /**
     * Called when the third token of the first line has been parsed.
     * 
     * @param token
     * @throws Exception
     */
    protected abstract void setParsedThirdToken(byte[] token) throws Exception;

    /**
     * Query whether or not this message is over the allowed change limit to
     * decide if we need to remarshall the entire thing.
     * 
     * @return boolean
     */
    protected boolean overChangeLimit() {
        return super.overHeaderChangeLimit();
    }

    /**
     * Called when the first CRLF is reached.
     * This should check to ensure correct tokens were all parsed and
     * throw an exception if they were not parsed.
     * 
     * @throws MalformedMessageException
     */
    protected abstract void parsingComplete() throws MalformedMessageException;

    /**
     * Query the first token to be marshalled outbound.
     * 
     * @return byte[]
     */
    protected abstract byte[] getMarshalledFirstToken();

    /**
     * Query the second token to be marshalled outbound.
     * 
     * @return byte[]
     */
    protected abstract byte[] getMarshalledSecondToken();

    /**
     * Query the third token to be marshalled outbound.
     * 
     * @return byte[]
     */
    protected abstract byte[] getMarshalledThirdToken();

    /**
     * Set the flag on whether the first line has been completely parsed yet
     * based on the input flag.
     * 
     * @param flag
     */
    protected void setFirstLineComplete(boolean flag) {
        this.completedFirstLine = flag;
        // once the first line is marked as completely parsed, reset the change
        // flag so that we truly know if it later changed
        if (flag) {
            this.firstLineChanged = false;
        }
    }

    /**
     * Query whether the first line has been completely parsed yet.
     * 
     * @return boolean
     */
    final protected boolean isFirstLineComplete() {
        return this.completedFirstLine;
    }

    /**
     * Set the flag that the first line has changed since being parsed.
     * 
     */
    protected void setFirstLineChanged() {
        this.firstLineChanged = true;
    }

    /**
     * Query whether or not the first line of the message has been changed
     * since the parse stage.
     * 
     * @return boolean
     */
    protected boolean hasFirstLineChanged() {
        return this.firstLineChanged || super.overHeaderChangeLimit();
    }

    /**
     * Query the number of first line tokens parsed for the incoming message.
     * 
     * @return int (0 if this was an outgoing message)
     */
    final protected int getNumberFirstLineTokens() {
        return this.numFirstLineTokensRead;
    }

    /**
     * Parse the first line of a message.
     * 
     * @param buff
     * @return boolean (true means parsed entire line)
     * @throws Exception
     */
    public boolean parseLine(WsByteBuffer buff) throws Exception {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseLine called for " + this);
        }

        //PI34161 - Record the start of the request at the time of parsing
        if ((this instanceof HttpRequestMessageImpl) && ((HttpRequestMessageImpl) this).getServiceContext().getHttpConfig().isAccessLoggingEnabled()) {
            this.startTime = System.nanoTime();
        }

        boolean rc = false;
        int startpos = (bTrace && tc.isDebugEnabled()) ? buff.position() : 0;
        TokenCodes tcRC = TokenCodes.TOKEN_RC_MOREDATA;

        // stop parsing the FirstLine when we either hit the end of it (CRLF)
        // or we hit the end of the buffer and need to read more
        while (!rc) {
            if (0 == this.numFirstLineTokensRead) {
                // first token, skip leading CRLFs (up to 16 blank lines)
                tcRC = skipCRLFs(buff);
                if (TokenCodes.TOKEN_RC_DELIM.equals(tcRC)) {
                    try {
                        tcRC = parseTokenExtract(buff, SPACE, false, LOG_FULL);
                        if (!tcRC.equals(TokenCodes.TOKEN_RC_MOREDATA)) {
                            setParsedFirstToken(getParsedToken());
                        }
                    } catch (MalformedMessageException mme) {
                        // no FFDC required

                        // debug print this failing first token to help figure
                        // out why the error happened, usually buffer corruption
                        // Note: if this was discrimination, then it should just
                        // mean it wasn't our data, but we can't tell here which
                        // path it was (i.e. should it have worked)
                        if (bTrace && tc.isDebugEnabled()) {
                            int curpos = buff.position();
                            buff.position(startpos);
                            byte[] data;
                            int offset = 0;
                            if (null != getParsedToken()) {
                                offset = getParsedToken().length;
                                data = new byte[(curpos - startpos) + offset];
                                System.arraycopy(getParsedToken(), 0, data, 0, offset);
                            } else {
                                data = new byte[(curpos - startpos)];
                            }
                            buff.get(data, offset, data.length - offset);
                            Tr.debug(tc, "Initial parse of message failed, (128) of buffer: \n"
                                         + GenericUtils.getHexDump(data, 128));
                        }
                        throw mme;
                    }
                } else if (TokenCodes.TOKEN_RC_MOREDATA.equals(tcRC)) {
                    // ran out of data
                    resetByteCache();
                    break; // out of while
                } else if (TokenCodes.TOKEN_RC_CRLF.equals(tcRC)) {
                    throw new MalformedMessageException("Too many leading CRLFs");
                }
            } else if (1 == this.numFirstLineTokensRead) {
                // second token
                tcRC = parseTokenExtract(buff, SPACE, true, LOG_PARTIAL);
                if (!tcRC.equals(TokenCodes.TOKEN_RC_MOREDATA)) {
                    setParsedSecondToken(getParsedToken());
                    if (tcRC.equals(TokenCodes.TOKEN_RC_CRLF)) {
                        if (bTrace && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Received CRLF after second token");
                        }
                        rc = true;
                    }
                }
            } else if (2 == this.numFirstLineTokensRead) {
                // third token
                tcRC = parseCRLFTokenExtract(buff, LOG_FULL);
                if (!tcRC.equals(TokenCodes.TOKEN_RC_MOREDATA)) {
                    setParsedThirdToken(getParsedToken());
                    rc = true;
                }
            } else {
                // stop coming here
                rc = true;
            }

            // scenarios where we need more data result in the MOREDATA RC above
            if (tcRC.equals(TokenCodes.TOKEN_RC_MOREDATA)) {
                resetByteCache();
                break; // out of while
            }

            // otherwise we finished parsing a single token
            setParsedToken(null);
            this.numFirstLineTokensRead++;
        } // end of while loop

        if (rc) {
            parsingComplete();
            setFirstLineComplete(true);
            // PK15898
            // go back one from the delimiter, unless it was an LF (if first
            // line ends with just an LF and not CRLF then we don't want to
            // change position as the header parsing above would consider it the
            // end of headers)
            decrementBytePositionIgnoringLFs();
        }

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseLine returning " + rc + " for " + this);
        }
        return rc;
    }

    /**
     * Marshall the first line.
     * 
     * @return WsByteBuffer[] of line ready to be written.
     */
    public WsByteBuffer[] marshallLine() {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallLine");
        }

        WsByteBuffer[] firstLine = new WsByteBuffer[1];
        firstLine[0] = allocateBuffer(getOutgoingBufferSize());
        firstLine = putBytes(getMarshalledFirstToken(), firstLine);
        firstLine = putByte(BNFHeaders.SPACE, firstLine);
        firstLine = putBytes(getMarshalledSecondToken(), firstLine);
        firstLine = putByte(BNFHeaders.SPACE, firstLine);
        firstLine = putBytes(getMarshalledThirdToken(), firstLine);
        firstLine = putBytes(BNFHeaders.EOL, firstLine);

        // don't flip the last buffer as headers get tacked on the end

        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallLine");
        }
        return firstLine;
    }

    /**
     * Parse a message from the input buffer. The input flag is whether or not
     * to save the header value immediately or delay the extraction until the
     * header value is queried.
     * 
     * @param buffer
     * @param bExtractValue
     * @return boolean (true means parsed entire message)
     * @throws Exception
     */
    public boolean parseMessage(WsByteBuffer buffer, boolean bExtractValue) throws Exception {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        boolean rc = false;
        if (!isFirstLineComplete()) {
            if (bTrace && tc.isDebugEnabled()) {
                Tr.debug(tc, "Parsing First Line");
            }
            rc = parseLine(buffer);
        }

        // if we've read the first line, then parse the headers
        // Note: we may come in with it true or it might be set to true above
        if (isFirstLineComplete()) {
            // keep parsing headers until that returns the "finished" response
            rc = parseHeaders(buffer, bExtractValue);
        }

        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "parseMessage returning " + rc);
        }
        return rc;
    }

    /**
     * Marshall a message.
     * 
     * @return WsByteBuffer[]
     * @throws MessageSentException
     */
    public WsByteBuffer[] marshallMessage() throws MessageSentException {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isEntryEnabled()) {
            Tr.entry(tc, "marshallMessage");
        }

        preMarshallMessage();
        WsByteBuffer[] marshalledObj = hasFirstLineChanged() ? marshallLine() : null;
        headerComplianceCheck();
        marshalledObj = marshallHeaders(marshalledObj);
        postMarshallMessage();

        if (bTrace && tc.isEntryEnabled()) {
            Tr.exit(tc, "marshallMessage");
        }
        return marshalledObj;
    }

    /**
     * Before marshalling headers into a buffer, this will run the data
     * through a compliancy check and take appropriate action (throw
     * errors, add missing headers, etc). Subclasses would write their
     * own according to their protocol
     * 
     * @throws MessageSentException
     */
    protected abstract void headerComplianceCheck() throws MessageSentException;

    /**
     * Before the message is marshalled, this method allows subclasses to
     * perform any final compliance checks or whatever else might be required
     * as last minute actions.
     * 
     * @throws MessageSentException
     */
    protected void preMarshallMessage() throws MessageSentException {
        // header marshalling uses the byte cache for performance so we need
        // to set that up before starting any marshalling code
        super.resetByteCache();
    }

    /**
     * After the message is marshalled, this method allows subclasses to
     * perform any cleanup they may want.
     */
    protected void postMarshallMessage() {
        // nothing the generic layer needs
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#clear()
     */
    @Override
    public void clear() {
        super.clear();
        this.completedFirstLine = false;
        this.numFirstLineTokensRead = 0;
        this.firstLineChanged = false;
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#destroy()
     */
    @Override
    protected void destroy() {
        super.destroy();
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#debug()
     */
    @Override
    public void debug() {
        // nothing extra here, subclasses should debug print the first
        // line information
        super.debug();
    }

    /**
     * Duplicate this generic message into the given object.
     * 
     * @param msg
     * @throws NullPointerException (if input message is null)
     */
    public void duplicate(GenericMessageImpl msg) {
        if (null == msg) {
            throw new NullPointerException("Null message passed to duplicate");
        }

        // first line information should be duplicated by the
        // subclasses for performance, so just duplicate the
        // headers
        super.duplicate(msg);
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException {

        super.readExternal(s);
        // nothing extra at this layer
    }

    /*
     * @see com.ibm.ws.genericbnf.internal.BNFHeadersImpl#writeExternal(java.io.
     * ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput s) throws IOException {
        super.writeExternal(s);
        // nothing extra at this layer
    }

}
