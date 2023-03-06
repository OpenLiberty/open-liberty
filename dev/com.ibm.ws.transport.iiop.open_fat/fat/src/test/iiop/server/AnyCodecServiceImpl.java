/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.server;

import java.rmi.RemoteException;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecFactory;
import org.omg.IOP.ENCODING_CDR_ENCAPS;
import org.omg.IOP.Encoding;
import org.osgi.service.component.annotations.Component;

import test.iiop.common.AnyCodecService;

@Component(immediate = true)
public class AnyCodecServiceImpl extends AbstractRemoteService<AnyCodecServiceImpl> implements AnyCodecService {
    @Override
    public byte[] getData(String s) throws RemoteException {
        try {
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 2);
            ORB orb = getOrb();
            CodecFactory codecFactory = (CodecFactory) orb.resolve_initial_references("CodecFactory");
            Codec codec = codecFactory.create_codec(encoding);
            Any any = ORB.init().create_any();
            // TODO Investigate interop issue with insert_wstring, insert_Value
            any.insert_string(s);
            byte[] data = codec.encode(any);
            return data;
        } catch (Exception e) {
            throw new RemoteException("Unexpected", e);
        }
    }
}
