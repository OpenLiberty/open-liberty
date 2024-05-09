package com.ibm.ws.wsoc.link;

public class LinkWriteFactory10 implements LinkWriteFactory {
    
    public LinkWrite getLinkWrite() {
        return new LinkWriteExt10();
    }
}
