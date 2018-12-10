package test.util;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.logging.Logger;

public class TestLogger extends TestWatcher {
    private static final String LF = "\n";
    private String HASHES = "################################################################################";

    private String pad(String s) {
        int len = s.length();
        if (len+4 > HASHES.length()) {
            do HASHES += HASHES; while(len+4 > HASHES.length());
            HASHES = HASHES.substring(0, len+4);
        }
        int pad = HASHES.length() - len;
        int lPad = pad / 2;
        int rPad = lPad + pad%2;
        return (lPad > 0 ? HASHES.substring(0, lPad) : "") + s + (rPad > 0 ? HASHES.substring(0, rPad) : "");
    }

    Logger log;

    @Override
    protected void starting(Description desc) {
        log = Logger.getLogger(desc.getDisplayName());
        final String s = pad(desc.getDisplayName());
        log.info(LF + LF + LF + HASHES + LF + s + LF + HASHES);
    }

    @Override
    protected void failed(Throwable e, Description desc) {
        log.info(LF + HASHES + LF + pad("FAILED") + LF + HASHES + LF + LF);
        log = null;
    }

    @Override
    protected void succeeded(Description desc) {
        log.info(LF + HASHES + LF + pad("PASSED") + LF + HASHES + LF + LF);
        log = null;
    }
}
