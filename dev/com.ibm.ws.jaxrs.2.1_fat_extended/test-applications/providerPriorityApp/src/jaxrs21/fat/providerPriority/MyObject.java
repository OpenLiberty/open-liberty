/**
 *
 */
package jaxrs21.fat.providerPriority;

public class MyObject {

    /*
     * This object would normally be serialized using XML or JSON but for this test
     * we are using MBWs and MBRs to send the data in plain text - the formatting of
     * that text is as follows:
     * Line 1: myString (required)
     * Line 2: myInt (required)
     * Line 3: mbrVersion (added by MBR before invoking resource)
     * Line 4: contextResolverVersionFromReader (added by MBR before invoking resource)
     * Line 5: mbwVersion (added by MBW after invoking resource)
     * Line 6: contextResolverVersionFromWriter (added by MBW after invoking resource)
     */
    private String myString;
    private int myInt;
    private int mbrVersion;
    private int mbwVersion;
    private int contextResolverVersionFromReader;
    private int contextResolverVersionFromWriter;

    public String getMyString() {
        return myString;
    }

    public void setMyString(String myString) {
        this.myString = myString;
    }

    public int getMyInt() {
        return myInt;
    }

    public void setMyInt(int myInt) {
        this.myInt = myInt;
    }

    public int getMbrVersion() {
        return mbrVersion;
    }

    public void setMbrVersion(int mbrVersion) {
        this.mbrVersion = mbrVersion;
    }

    public int getMbwVersion() {
        return mbwVersion;
    }

    public void setMbwVersion(int mbwVersion) {
        this.mbwVersion = mbwVersion;
    }

    public int getContextResolverVersionFromReader() {
        return contextResolverVersionFromReader;
    }

    public void setContextResolverVersionFromReader(int contextResolverVersionFromReader) {
        this.contextResolverVersionFromReader = contextResolverVersionFromReader;
    }

    public int getContextResolverVersionFromWriter() {
        return contextResolverVersionFromWriter;
    }

    public void setContextResolverVersionFromWriter(int contextResolverVersionFromWriter) {
        this.contextResolverVersionFromWriter = contextResolverVersionFromWriter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(myString).append("|").append(myInt);
        if (mbrVersion > 0) {
            sb.append("|").append(mbrVersion);
            if (contextResolverVersionFromReader > 0) {
                sb.append("|").append(contextResolverVersionFromReader);
                if (mbwVersion > 0) {
                    sb.append("|").append(mbwVersion);
                    if (contextResolverVersionFromWriter > 0) {
                        sb.append("|").append(contextResolverVersionFromWriter);
                    }
                }
            }
        }
        return sb.toString();
    }
}
