/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.jcdi.util.html;

import java.nio.CharBuffer;
import java.util.List;

/**
 * Shared common code for html parsing and rewriting.
 * 
 * @author yingwang
 * 
 */
public class HtmlDefaultParser implements AbstractParser {

    static boolean DEBUG = false;
    /**
     * The state of this parser.
     */
    int state;

    /**
     * Where we start to handle stored + new char buffers. it should be the length
     * of stored char buffer;
     */
    int savedIndex;

    /**
     * This buffer is used to store the partial tag that may span 2 char buffers.
     */
    CharBuffer savedCharBuffer;

    /**
     * The callback to handle the html events.
     */
    HtmlDefaultEventHandler handler;

    @Override
    public void reset(Object handler) {
        this.reset((HtmlDefaultEventHandler) handler);
    }

    /**
     * Reset the the parser and attach new handler to the parser.
     * 
     * @param handler The html event handler.
     */
    public void reset(HtmlDefaultEventHandler handler) {
        if (DEBUG)
            System.out.println("HTMLAP.reset() handler=" + handler);
        this.handler = handler;
        this.handler.reset();
        state = State.STATE_CONTENTS;
        savedIndex = -1;
        savedCharBuffer = null;
        if (DEBUG)
            System.out.println("HTMLAP.reset() exit.");
    }

    /**
     * The simple parser. Since html might have open tags, and xml SAX parser does not support nio,
     * we need to implement ourselves. This simple html parser will fire following events:
     * 
     * 1. Tags, the charbuffer between/include < and >
     * 2. DTD style tags, the charbuffer between/include <! and >
     * 3. Comments, the char buffer between/include <!-- and -->
     * 4. Normal text contents, note the parser might fire multiple events for a block of text contents.
     * 
     * The parser is designed for junction rewriting features and could be extended for other rewriting
     * purpose.
     * 
     * @param inputBuffers the input char buffers.
     * @param outputBuffers The output char buffers.
     * 
     * @throws Exception any exceptions that fired from the html event handler.
     * 
     */
    @Override
    public void parse(CharBuffer inputBuffers[], List<CharBuffer> outputBuffers) throws Exception {
        int index, start;
        CharBuffer currentBuffer = null, valueBuffer, resultBuffer;

        if (DEBUG)
            System.out.println("HTMLAP.parse() inputBuffers=" + inputBuffers +
                               " outputBuffers=" + outputBuffers +
                               " state=" + State.STATES[state] +
                               " savedIndex=" + savedIndex);

        for (CharBuffer inputBuffer : inputBuffers) {

            // If saved buffer is not null, we need to merge the saved and the first buffer in the input list.
            if (savedCharBuffer != null) {
                if (DEBUG)
                    System.out.println("HTMLAP.parse() savedCharBuffer.remaining=" + savedCharBuffer.remaining() +
                                       " inputbuffer.remaining()=" + inputBuffer.remaining());
                currentBuffer = CharBuffer.allocate(savedCharBuffer.remaining() + inputBuffer.remaining() + 1);
                savedCharBuffer.read(currentBuffer);
                inputBuffer.read(currentBuffer);
                currentBuffer.flip();
                savedCharBuffer = null;
            } else {
                currentBuffer = inputBuffer;
            }

            int position = start = index = currentBuffer.position();
            if (DEBUG)
                System.out.println("HTMLAP.parse() currentBuffer.remaining()=" + currentBuffer.remaining() +
                                   " currentBuffer.limit()=" + currentBuffer.limit() +
                                   " position=start=index=" + position +
                                   " savedIndex=" + savedIndex);
            while (index < currentBuffer.limit()) {
                if (handler.isFinished()) {
                    if (DEBUG)
                        System.out.println("HTMLAP.parse() handler=" + handler + " is finished. outputBuffers.size()=" +
                                           outputBuffers.size());
                    return;
                }
                if (DEBUG)
                    System.out.println("HTMLAP.parse() state=" + State.STATES[state] +
                                       " start=" + start + " index=" + index);
                switch (state) {
                    case State.STATE_CONTENTS:
                        // handle the normal contents between tags. We do not buffer the whole
                        // contents between 2 tags because we do not rewrite the contents.
                        if (currentBuffer.get(start) == '<') {
                            state = State.STATE_LTAG;
                            start = index;
                            continue;
                        }
                        for (index = (savedIndex > 0 ? start + savedIndex : start); index < currentBuffer.limit(); index++) {
                            if (currentBuffer.get(index) == '<')
                                break;
                        }
                        if (index < currentBuffer.limit()) {
                            valueBuffer = CharBuffer.wrap(currentBuffer, start - position, index - position);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() firing handler.handleContents0 on valueBuffer=" + valueBuffer);
                            resultBuffer = handler.handleContents(valueBuffer);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() handler.handleContents0 resultBuffer=" + resultBuffer);
                            if (resultBuffer != null) {
                                outputBuffers.add(resultBuffer);
                            }
                            savedIndex = -1;
                            state = State.STATE_LTAG;
                            start = index;
                            continue;
                        }
                        valueBuffer = CharBuffer.wrap(currentBuffer, start - position, currentBuffer.limit() - position);
                        if (DEBUG)
                            System.out.println("HTMLAP.parse() firing handler.handleContents1 on valueBuffer=" + valueBuffer);
                        resultBuffer = handler.handleContents(valueBuffer);
                        if (DEBUG)
                            System.out.println("HTMLAP.parse() handler.handleContents1 resultBuffer=" + resultBuffer);
                        index = currentBuffer.limit();
                        if (resultBuffer != null) {
                            outputBuffers.add(resultBuffer);
                        }
                        savedIndex = -1;
                        continue;

                    case State.STATE_LTAG:
                        if (currentBuffer.limit() - start == 1) {
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() STATE_LTAG saveRemaining0 start=" + start +
                                                   "currentBuffer.limit()=" + currentBuffer.limit());
                            saveRemaining(currentBuffer, start);
                            index = currentBuffer.limit();
                            continue;
                        }
                        if (currentBuffer.get(start + 1) == '!') {
                            state = State.STATE_LDTD;
                            savedIndex = -1;
                            continue;
                        } else {
                            for (index = (savedIndex > 0 ? start + savedIndex : start + 1); index < currentBuffer.limit(); index++) {
                                if (currentBuffer.get(index) == '>') {
                                    break;
                                }
                            }
                        }

                        if (index < currentBuffer.limit()) {
                            index++;
                            valueBuffer = CharBuffer.wrap(currentBuffer, start - position, index - position);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() firing handler.handleTag on valueBuffer=" + valueBuffer);
                            resultBuffer = handler.handleTag(valueBuffer);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() handler.handleTag resultBuffer=" + resultBuffer);
                            if (resultBuffer != null) {
                                outputBuffers.add(resultBuffer);
                            }
                            start = index;
                            state = State.STATE_CONTENTS;
                            savedIndex = -1;
                            continue;
                        }
                        if (DEBUG)
                            System.out.println("HTMLAP.parse() STATE_LTAG saveRemaining1 start=" + start +
                                               "currentBuffer.limit()=" + currentBuffer.limit());
                        saveRemaining(currentBuffer, start);
                        index = currentBuffer.limit();
                        continue;
                    case State.STATE_LDTD:
                        if (currentBuffer.limit() - start <= 2) {
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() STATE_LDTD saveRemaining0 start=" + start +
                                                   "currentBuffer.limit()=" + currentBuffer.limit());
                            saveRemaining(currentBuffer, start);
                            index = currentBuffer.limit();
                            continue;
                        }
                        if (currentBuffer.get(start + 2) == '-') {
                            if (currentBuffer.limit() - index <= 3) {
                                if (DEBUG)
                                    System.out.println("HTMLAP.parse() STATE_LDTD saveRemaining1 start=" + start +
                                                       "currentBuffer.limit()=" + currentBuffer.limit());
                                saveRemaining(currentBuffer, start);
                                index = currentBuffer.limit();
                                continue;
                            }
                            if (currentBuffer.get(start + 3) == '-') {
                                state = State.STATE_LCOMMENTS;
                                savedIndex = -1;
                                continue;
                            }
                            for (index = (savedIndex > 0 ? start + savedIndex : start + 3); index < currentBuffer.limit(); index++) {
                                if (currentBuffer.get(index) == '>') {
                                    break;
                                }
                            }
                        } else {
                            for (index = (savedIndex > 0 ? start + savedIndex : start + 2); index < currentBuffer.limit(); index++) {
                                if (currentBuffer.get(index) == '>') {
                                    break;
                                }
                            }
                        }
                        if (index < currentBuffer.limit()) {
                            index++;
                            valueBuffer = CharBuffer.wrap(currentBuffer, start - position, index - position);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() firing handler.handleDTD on valueBuffer=" + valueBuffer);
                            resultBuffer = handler.handleDTD(valueBuffer);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() handler.handleDTD resultBuffer=" + resultBuffer);
                            if (resultBuffer != null) {
                                outputBuffers.add(resultBuffer);
                            }
                            start = index;
                            state = State.STATE_CONTENTS;
                            savedIndex = -1;
                            continue;
                        }
                        if (DEBUG)
                            System.out.println("HTMLAP.parse() STATE_LDTD saveRemaining2 start=" + start +
                                               "currentBuffer.limit()=" + currentBuffer.limit());
                        saveRemaining(currentBuffer, start);
                        index = currentBuffer.limit();
                        continue;
                    case State.STATE_LCOMMENTS:
                        if (currentBuffer.limit() - start <= 4) {
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() STATE_LCOMMENTS saveRemaining0 start=" + start +
                                                   "currentBuffer.limit()=" + currentBuffer.limit());
                            saveRemaining(currentBuffer, start);
                            index = currentBuffer.limit();
                            continue;
                        }

                        for (index = (savedIndex > 0 ? start + savedIndex : start + 4); index < currentBuffer.limit(); index++) {
                            if (currentBuffer.get(index) == '>') {
                                if (currentBuffer.get(index - 1) == '-' && index - 1 > start + 5) {
                                    if (currentBuffer.get(index - 2) == '-' && index - 2 > start + 4) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (index < currentBuffer.limit()) {
                            index++;
                            valueBuffer = CharBuffer.wrap(currentBuffer, start - position, index - position);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() firing handler.handleComments on valueBuffer=" + valueBuffer);
                            resultBuffer = handler.handleComments(valueBuffer);
                            if (DEBUG)
                                System.out.println("HTMLAP.parse() handler.handleComments resultBuffer=" + resultBuffer);
                            if (resultBuffer != null) {
                                outputBuffers.add(resultBuffer);
                            }
                            start = index;
                            state = State.STATE_CONTENTS;
                            savedIndex = -1;
                            continue;
                        }
                        if (DEBUG)
                            System.out.println("HTMLAP.parse() STATE_LCOMMENTS saveRemaining1 start=" + start +
                                               "currentBuffer.limit()=" + currentBuffer.limit());
                        saveRemaining(currentBuffer, start);
                        index = currentBuffer.limit();
                        continue;
                } // swith
            } // while
        } // for
        if (DEBUG)
            System.out.println("HTMLAP.parse() outputBuffers.size()=" + outputBuffers.size());
    }

    /**
     * Save remaining buffer from the absolute index 'from'.
     * 
     * @param charBuffer the unfinished char buffer.
     * @param from the absolute index from.
     * 
     * @throws Exception index out of bound.
     */
    protected void saveRemaining(CharBuffer charBuffer, int from) throws Exception {
        savedCharBuffer = CharBuffer.wrap(charBuffer, from - charBuffer.position(), charBuffer.limit() - charBuffer.position());
        savedIndex = savedCharBuffer.remaining();
    }

    /**
     * This simple parser's state.
     * 
     * @author yingwang
     * 
     */
    protected static class State {

        /**
         * normal text block between <tag>tags</tag>.
         */
        static final int STATE_CONTENTS = 1;

        /**
         * For "<"
         */
        static final int STATE_LTAG = 2;

        /**
         * For "<!"
         */
        static final int STATE_LDTD = 3;

        /**
         * For "<!--"
         */
        static final int STATE_LCOMMENTS = 4;

        /**
         * The state names.
         */
        static final String STATES[] = {
                                        "INVALID",
                                        "STATE_CONTENTS",
                                        "STATE_LTAG",
                                        "STATE_LDTD",
                                        "STATE_LCOMMENTS",
                                        "INVALID"
        };
    }

    /**
     * Null html event handler.
     * 
     * @author yingwang
     * 
     */
    public static class NullHtmlEventHandler implements HtmlDefaultEventHandler {

        @Override
        public void startDoc() {}

        @Override
        public CharBuffer handleDTD(CharBuffer buffer) {
            return buffer;

        }

        @Override
        public CharBuffer handleTag(CharBuffer buffer) {
            return buffer;

        }

        @Override
        public CharBuffer handleContents(CharBuffer buffer) {
            return buffer;

        }

        @Override
        public CharBuffer handleComments(CharBuffer buffer) {
            return buffer;
        }

        @Override
        public void endDoc() {

        }

        @Override
        public CharBuffer[] flush() {
            return null;
        }

        @Override
        public void reset() {

        }

        @Override
        public boolean isFinished() {
            return false;
        }
    }

    /**
     * unit test cases.
     * 
     * @param args
     */
    static public void main(String args[]) {
        CharBuffer[] inputBuffers = new CharBuffer[] {
                                                      CharBuffer.wrap("    <a href=\"../../java/nio/CharBuffer.html#as"),
                                                      CharBuffer.wrap("ReadOnlyBuffer%28%29\">asReadOnlyBuffer</a></b><"),
                                                      CharBuffer.wrap("a code=\"333><!"),
                                                      CharBuffer.wrap("--// document.write('<script src=\"http://www.wforum.com/ads/real.js\"></script>');//-->")
        };
        inputBuffers[0].position(2);
        HtmlDefaultParser parser = new HtmlDefaultParser();
        parser.reset(new NullHtmlEventHandler());
        List<CharBuffer> outputBuffers = new java.util.ArrayList<CharBuffer>();
        try {
            parser.parse(inputBuffers, outputBuffers);
            if (DEBUG)
                System.out.print(outputBuffers);
        } catch (Exception ex) {

        }
    }
}
