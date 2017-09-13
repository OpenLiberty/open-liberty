package com.ibm.ws.ras;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;

import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation;

public class StaticTraceTestTransformer extends LibertyTracePreprocessInstrumentation {

    StaticTraceTestTransformer() {
        super();

    }
    
    
    /**
     * Instrument the classes.
     */
    public byte[] transform(byte[] classfileBuffer) throws IllegalClassFormatException {
    	//setFfdc(true);
    	
        try {
            return transform(new ByteArrayInputStream(classfileBuffer));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

}
