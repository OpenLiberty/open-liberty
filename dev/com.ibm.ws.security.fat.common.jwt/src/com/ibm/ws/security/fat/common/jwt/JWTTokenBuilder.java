/*
 * IBM Confidential OCO Source Material
 * 5630-A36 (C) COPYRIGHT International Business Machines Corp.  2019
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 *
 */
package com.ibm.ws.security.fat.common.jwt;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.jose4j.base64url.SimplePEMEncoder;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;

/**
 * Convenience class to build jwt tokens to test consumption of them by WebSphere.
 * Uses a constant public and private key, so the public key only has to
 * be added to WebSphere's trust store once.
 *
 * Another way to get these tokens would have been to define a bunch of jwtbuilders
 * on the OP and call their "token" endpoints.
 *
 * To add the public key to a websphere trust store, proceed as follows:
 * copy the output of getPrivateKeyPem() to privatekey.pem.
 * openssl req -x509 -key privateKey.pem -nodes -days 3650 -newkey rsa:2048 -out temp.pem
 * openssl x509 -outform der -in temp.pem -out temp.der
 * then use ikeyman or keytool to add signer temp.der to key.jks, perhaps like this:
 * keytool -importcert \
 * -file <certificate to trust> \
 * -alias <alias for the certificate> \
 * -keystore <name of the trustore> \
 * -storepass <password for the truststore> \
 * -storetype jks
 *
 * @author bruce
 *
 */
public class JWTTokenBuilder {
    JwtClaims _claims = null;
    JsonWebSignature _jws = null;
    RsaJsonWebKey _rsajwk = null;
    String _jwt = null;
    private final Key _signingKey = null;
    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";
//  private static final String BEGIN_PRIV_KEY = "-----BEGIN RSA PRIVATE KEY-----";
//  private static final String END_PRIV_KEY = "-----END RSA PRIVATE KEY-----";
    private static final String BEGIN_PRIV_KEY = "-----BEGIN PRIVATE KEY-----";
    private static final String END_PRIV_KEY = "-----END PRIVATE KEY-----";
    private static final String pubKey = "-----BEGIN PUBLIC KEY-----\n" +
                                         "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgDXSu5Bp6zhitr3cwkRD\n" +
                                         "zBGmnWbBjUqpFSsKgbQxFUBfY4zD1XeUampfv+gWn0i8asVYID2EAa+Xd6i9EF6x\n" +
                                         "dEhISEtKI10bH2xXZe4iHzOb26s2nkw9X/1P+NiIGOtm5chd5Ze9OwSTECRHcURb\n" +
                                         "e6ym0SkYknqzVDi+d9QSnuhcLrK7OhMMgxaT4+aN1XD64oobZ9aeI7ANHc1/b6wn\n" +
                                         "pE8OhpQERql5CPf0HizZUDl/raPQCT+GmxabQ2pR98+f8qfI1mUXD0tcc9yOQKnj\n" +
                                         "UcGE2tTmz1wpHXvi/W70rWkQDLUYR9xmxg3YudW2cfweLd5REe57ymw16FWY6rAH\n" +
                                         "vQIDAQAB\n" +
                                         "-----END PUBLIC KEY-----";

//    private static final String privKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
//                                          "MIIJKAIBAAKCAgEAt8rYXqRbYXVYukHWjjI2+4gYL3gJMh4YIaChfNHOJnlL18zE\n" +
//                                          "3eN6IJFJXKeyOvDXbg1mZm83DDNa5FLV2XGVCfpuAYHv3DSdaj8kwf48nLtVW0j8\n" +
//                                          "hOc7LNgJkKTFyQsV47StPmsYFJstuCCjV1p0chmSZwSSC5f3r/LZLhb1dUbuxlrf\n" +
//                                          "6HZJvjx+cpKyfQ1ZtEkXsj0Gtf2l/8GF2ADyN2a+jLelF4xIOTashg+suiKXX2qt\n" +
//                                          "QBAgwBhDNngc0yRfzAwlAgKpnbQs1x/7eJMIcKvr9rcBrnnlK/5NV6s3gFtT56F6\n" +
//                                          "oWnOrvRMweWt4ADBHvlQTVYPVPIHnXpaY+1wqi1TAkW8lsaY/u/pmiLQbX8C44N5\n" +
//                                          "qE4F9c2WtcqsnMArFtZZb/QMnSPxv6D3ZPmbzvrd0dJL3VdD9OsL5ivJjt5vx3ag\n" +
//                                          "L0cg9fFpw8P7msL21YYJvXIfXawY5RaY9rrsab0+bUmp9sbYYW4Dm7PdL5mC3Q9z\n" +
//                                          "DtPfwJAaSZSSelNEd+Dm1X9hFmKPxeRLKDpm/+clC+qdRmibXJYe0kxU/WulKeSc\n" +
//                                          "tOEiYPvkrjsAakcHTMVTnl3unR8CIdF68CjTGv2vCOe85QO4eMWZDfbNtaT6ldhr\n" +
//                                          "Gezp4oUKgXbUPuklMFeBOqKol/tOWWE5U1ShBryUwsD1M0AWa2utu77GKyMCAwEA\n" +
//                                          "AQKCAgB/PdPmUeth4Zf3+zTLZUBji6kS1AupwuP6A/dJLweF41v6ny18YMxgV7Vu\n" +
//                                          "jDr8S1kaO/S03jJCJBu9Dww2akhAjDw0lvy0e0iwEd8k3xqfd6J0fGc2q89wEHEu\n" +
//                                          "zqSPtzkEWEXIfORC/9v7Kb1r8JfFlqag460okaFNEtgA7Kwq8VzFvoxdp+cN6Vux\n" +
//                                          "fpZLtT5xblkVcOKAhIa0NgqvrJgEewFY8ps9XkVyNsPiXF+8zc32pSgkvgWueB2w\n" +
//                                          "TB/IndokrhPA/I7pO5//n4i9xS2Gs7+9Ip3raUCb4IczNrpKBvBj4IvBzDrnLQMn\n" +
//                                          "ChgCiQMqqnhJ0lby/E0BXX/OxsfEHRu3cuquD/09t+Mf24vk0YN17nl4NCsEHhwz\n" +
//                                          "FBVDf2Uayv5T9Waoi/34M0tX5EwkAxb74+XVOonadPZskFjDuZZbVFeZCV50nHFx\n" +
//                                          "PUOBg9eZ3UsDgl1e5PppWOpS0zXcJs4JKI3dMzRqIff/fCFwsVN4Km+NkXz4Sykg\n" +
//                                          "q/s/q9yi4Cq5y9F8BE46er4JOP8CNm5wURwPq4Q5vGERZP58Y9+SuXQDcg86qRf+\n" +
//                                          "NStNuwAR6uFxNZhLjLsleDVu5sF12PQOBPzf+0B/ByvPZw0hzx1egF7jitfmZHSZ\n" +
//                                          "0lPrZQaCn2TYO89iazhIJ6BW2TZa+zJR7HiaoUtCCF0EiKziwQKCAQEA2cvIhuqg\n" +
//                                          "tyenSddHewS/poPoQXZLJZBqc3sbFJrFePCY0T/6EtwnB5b+AdwUw5Y1Wo1rxZIi\n" +
//                                          "S/R5MO7DjBFxAW5KKVWcHUVaq0F5LrC1smQQICtZ/khUeC4FoMNtOFfpnGiLrOoT\n" +
//                                          "gC4gO043woEZlOi+s7yeTwyCOp9+Ivr+hmX+DnuZ9kYT3YYYyWfHc75vP/5QFNi6\n" +
//                                          "80fGnLrtte3XQynKYgm9aiWHXES4dwrNTk6jMx6nSaYckNf8/3Ph2n8z4NrAQv0I\n" +
//                                          "4atlFymQb2BfQbzC6PaHlpEax5WGwuxgVptwbgiPFRZQrAxhpsbZn5O//FIgbKuI\n" +
//                                          "B2O49GrF7KalCQKCAQEA2AgdKH+rS/oIeGjmIPKErALPX3JMRGdqH8PlAbgQWfrb\n" +
//                                          "orE+KImknEitcA2NA460dxBJMLacGZoRdNJyJDEQPa36ooSRwj8aHjLd4CwYUpTQ\n" +
//                                          "sy9iUPdHqRaZNPM9STSy4dVFqydybNHyK6114mnwT+nvyaXUJ7DzfpHcdFh5Ew6O\n" +
//                                          "D9Hu1PD8wSYetjpE0YkQLqEWVtZwBE023XTgmo5pSZ0IBuFvF17RaBcVQ+T8bOV0\n" +
//                                          "5qnlAcSlz5l21vnyxU80fPCXUCrJSYWDwsbXImH6+1xw4slLzKgUpzWpE6f3h0Py\n" +
//                                          "HgPVJNp8mlRXpaLCU1JEx1y3r7NRv0+2nuXnO08lywKCAQBso/VUL2eZ1SLid4uF\n" +
//                                          "dtnuwu+w78QOadpf3nGktot4h3ODNYmVrNGfPJdZ8CE/awcUM2Ul+X44Kyvk18Ud\n" +
//                                          "cnnPP+eodLbZ9wWCVbeQLb+Ey9srYNSUPho8lKBkD/fEWj4CsjeyOlUd5GRZkOvJ\n" +
//                                          "j0JmgC7YU2cYgWHYwkRWSKN8ARZYvRkQuuYf40sr9COOvdiasE3cCDxBLHWLKsd2\n" +
//                                          "r7xoGUmF29vrPesmgHUPlIjS3fEUh97kowKu0b624mQv7LHBIP00rSmoACn+AtK0\n" +
//                                          "a/s4PGNxd8AswBb+pbzCMMzhhsnvaT+OQkJdgBOZ8KkPq9DhmUnpSysgnILfEP/H\n" +
//                                          "y1yZAoIBAA0UcK+DC1wOR3UC5OwZwu6nPOcKhJOfr009DyCLhHHuPl4bXSgXLWBJ\n" +
//                                          "BjdCmsccXNDYq5XHeKwUJ/pqw35teg5B+mrcm/am2234pnZsNQzK9dfjhpBgaHZU\n" +
//                                          "Z/JMx+kmx63ku9MhEEyGaaM7XWfYAjTUdTBAWhgNHrELI83njW0Z9IAAtfUuoh8P\n" +
//                                          "r8xYutH0+oXYOwIG+cFI64l9ChxRgw5x84p3G8LOet9ShncV5jKxseJFZxg0T9XB\n" +
//                                          "9PAPMiPAJ+1oo8C21nkdHF0urNfoFsohTrRse6pogtec6B/Ii7Qk6QPoN3+Duwed\n" +
//                                          "E7FzqVBiKsfnVTfOI6TxrpzwTo/IuwECggEBANNh+E9bsnx16RGPwxy2nwdQMe5Z\n" +
//                                          "s+HwL1rsbZOZ/0CTn/RbhpsRCU7nX/G28aSQyvWCxXHIlOjWGEam/wBzzqSqP4GB\n" +
//                                          "KLfk84Ar1fFqBhs4PskCfFgxK7y7Q2T/ZWlj74qbrfXCmnPIALuxDpoLrOK2781w\n" +
//                                          "XHNDag45HLHu6dSpQw4Vjeg1+svRCazSDsVLk7hAW59My+BNkEhsakO2RmWFfA8f\n" +
//                                          "YjKpUaixs1hhkGn2gGmV/236fCA3W6ME/jUBeID6veQLc2NxsvPdIbgwPJ6jaIeO\n" +
//                                          "vSPlYvIpINcfeDThYUgt3CzbhITtFyHS6+WejIU5mr03p8r3QyYbIEVf7UE=\n" +
//                                          "-----END RSA PRIVATE KEY-----";

//    // somewhat good
//    private static final String privKey = "-----BEGIN PRIVATE KEY-----\n" +
//                                          "MIIJQgIBADANBgkqhkiG9w0BAQEFAASCCSwwggkoAgEAAoICAQC3ythepFthdVi6\n" +
//                                          "QdaOMjb7iBgveAkyHhghoKF80c4meUvXzMTd43ogkUlcp7I68NduDWZmbzcMM1rk\n" +
//                                          "UtXZcZUJ+m4Bge/cNJ1qPyTB/jycu1VbSPyE5zss2AmQpMXJCxXjtK0+axgUmy24\n" +
//                                          "IKNXWnRyGZJnBJILl/ev8tkuFvV1Ru7GWt/odkm+PH5ykrJ9DVm0SReyPQa1/aX/\n" +
//                                          "wYXYAPI3Zr6Mt6UXjEg5NqyGD6y6Ipdfaq1AECDAGEM2eBzTJF/MDCUCAqmdtCzX\n" +
//                                          "H/t4kwhwq+v2twGueeUr/k1XqzeAW1PnoXqhac6u9EzB5a3gAMEe+VBNVg9U8ged\n" +
//                                          "elpj7XCqLVMCRbyWxpj+7+maItBtfwLjg3moTgX1zZa1yqycwCsW1llv9AydI/G/\n" +
//                                          "oPdk+ZvO+t3R0kvdV0P06wvmK8mO3m/HdqAvRyD18WnDw/uawvbVhgm9ch9drBjl\n" +
//                                          "Fpj2uuxpvT5tSan2xthhbgObs90vmYLdD3MO09/AkBpJlJJ6U0R34ObVf2EWYo/F\n" +
//                                          "5EsoOmb/5yUL6p1GaJtclh7STFT9a6Up5Jy04SJg++SuOwBqRwdMxVOeXe6dHwIh\n" +
//                                          "0XrwKNMa/a8I57zlA7h4xZkN9s21pPqV2GsZ7OnihQqBdtQ+6SUwV4E6oqiX+05Z\n" +
//                                          "YTlTVKEGvJTCwPUzQBZra627vsYrIwIDAQABAoICAH890+ZR62Hhl/f7NMtlQGOL\n" +
//                                          "qRLUC6nC4/oD90kvB4XjW/qfLXxgzGBXtW6MOvxLWRo79LTeMkIkG70PDDZqSECM\n" +
//                                          "PDSW/LR7SLAR3yTfGp93onR8Zzarz3AQcS7OpI+3OQRYRch85EL/2/spvWvwl8WW\n" +
//                                          "pqDjrSiRoU0S2ADsrCrxXMW+jF2n5w3pW7F+lku1PnFuWRVw4oCEhrQ2Cq+smAR7\n" +
//                                          "AVjymz1eRXI2w+JcX7zNzfalKCS+Ba54HbBMH8id2iSuE8D8juk7n/+fiL3FLYaz\n" +
//                                          "v70inetpQJvghzM2ukoG8GPgi8HMOuctAycKGAKJAyqqeEnSVvL8TQFdf87Gx8Qd\n" +
//                                          "G7dy6q4P/T234x/bi+TRg3XueXg0KwQeHDMUFUN/ZRrK/lP1ZqiL/fgzS1fkTCQD\n" +
//                                          "Fvvj5dU6idp09myQWMO5lltUV5kJXnSccXE9Q4GD15ndSwOCXV7k+mlY6lLTNdwm\n" +
//                                          "zgkojd0zNGoh9/98IXCxU3gqb42RfPhLKSCr+z+r3KLgKrnL0XwETjp6vgk4/wI2\n" +
//                                          "bnBRHA+rhDm8YRFk/nxj35K5dANyDzqpF/41K027ABHq4XE1mEuMuyV4NW7mwXXY\n" +
//                                          "9A4E/N/7QH8HK89nDSHPHV6AXuOK1+ZkdJnSU+tlBoKfZNg7z2JrOEgnoFbZNlr7\n" +
//                                          "MlHseJqhS0IIXQSIrOLBAoIBAQDZy8iG6qC3J6dJ10d7BL+mg+hBdkslkGpzexsU\n" +
//                                          "msV48JjRP/oS3CcHlv4B3BTDljVajWvFkiJL9Hkw7sOMEXEBbkopVZwdRVqrQXku\n" +
//                                          "sLWyZBAgK1n+SFR4LgWgw204V+mcaIus6hOALiA7TjfCgRmU6L6zvJ5PDII6n34i\n" +
//                                          "+v6GZf4Oe5n2RhPdhhjJZ8dzvm8//lAU2LrzR8acuu217ddDKcpiCb1qJYdcRLh3\n" +
//                                          "Cs1OTqMzHqdJphyQ1/z/c+HafzPg2sBC/Qjhq2UXKZBvYF9BvMLo9oeWkRrHlYbC\n" +
//                                          "7GBWm3BuCI8VFlCsDGGmxtmfk7/8UiBsq4gHY7j0asXspqUJAoIBAQDYCB0of6tL\n" +
//                                          "+gh4aOYg8oSsAs9fckxEZ2ofw+UBuBBZ+tuisT4oiaScSK1wDY0DjrR3EEkwtpwZ\n" +
//                                          "mhF00nIkMRA9rfqihJHCPxoeMt3gLBhSlNCzL2JQ90epFpk08z1JNLLh1UWrJ3Js\n" +
//                                          "0fIrrXXiafBP6e/JpdQnsPN+kdx0WHkTDo4P0e7U8PzBJh62OkTRiRAuoRZW1nAE\n" +
//                                          "TTbddOCajmlJnQgG4W8XXtFoFxVD5Pxs5XTmqeUBxKXPmXbW+fLFTzR88JdQKslJ\n" +
//                                          "hYPCxtciYfr7XHDiyUvMqBSnNakTp/eHQ/IeA9Uk2nyaVFelosJTUkTHXLevs1G/\n" +
//                                          "T7ae5ec7TyXLAoIBAGyj9VQvZ5nVIuJ3i4V22e7C77DvxA5p2l/ecaS2i3iHc4M1\n" +
//                                          "iZWs0Z88l1nwIT9rBxQzZSX5fjgrK+TXxR1yec8/56h0ttn3BYJVt5Atv4TL2ytg\n" +
//                                          "1JQ+GjyUoGQP98RaPgKyN7I6VR3kZFmQ68mPQmaALthTZxiBYdjCRFZIo3wBFli9\n" +
//                                          "GRC65h/jSyv0I4692JqwTdwIPEEsdYsqx3avvGgZSYXb2+s96yaAdQ+UiNLd8RSH\n" +
//                                          "3uSjAq7RvrbiZC/sscEg/TStKagAKf4C0rRr+zg8Y3F3wCzAFv6lvMIwzOGGye9p\n" +
//                                          "P45CQl2AE5nwqQ+r0OGZSelLKyCcgt8Q/8fLXJkCggEADRRwr4MLXA5HdQLk7BnC\n" +
//                                          "7qc85wqEk5+vTT0PIIuEce4+XhtdKBctYEkGN0Kaxxxc0Nirlcd4rBQn+mrDfm16\n" +
//                                          "DkH6atyb9qbbbfimdmw1DMr11+OGkGBodlRn8kzH6SbHreS70yEQTIZpoztdZ9gC\n" +
//                                          "NNR1MEBaGA0esQsjzeeNbRn0gAC19S6iHw+vzFi60fT6hdg7Agb5wUjriX0KHFGD\n" +
//                                          "DnHzincbws5631KGdxXmMrGx4kVnGDRP1cH08A8yI8An7WijwLbWeR0cXS6s1+gW\n" +
//                                          "yiFOtGx7qmiC15zoH8iLtCTpA+g3f4O7B50TsXOpUGIqx+dVN84jpPGunPBOj8i7\n" +
//                                          "AQKCAQEA02H4T1uyfHXpEY/DHLafB1Ax7lmz4fAvWuxtk5n/QJOf9FuGmxEJTudf\n" +
//                                          "8bbxpJDK9YLFcciU6NYYRqb/AHPOpKo/gYEot+TzgCvV8WoGGzg+yQJ8WDErvLtD\n" +
//                                          "ZP9laWPviput9cKac8gAu7EOmgus4rbvzXBcc0NqDjkcse7p1KlDDhWN6DX6y9EJ\n" +
//                                          "rNIOxUuTuEBbn0zL4E2QSGxqQ7ZGZYV8Dx9iMqlRqLGzWGGQafaAaZX/bfp8IDdb\n" +
//                                          "owT+NQF4gPq95AtzY3Gy890huDA8nqNoh469I+Vi8ikg1x94NOFhSC3cLNuEhO0X\n" +
//                                          "IdLr5Z6MhTmavTenyvdDJhsgRV/tQQ==\n" +
//                                          "-----END PRIVATE KEY-----";
//
    private static final String privKey = "-----BEGIN PRIVATE KEY-----\n" +
                                          "MIIJQgIBADANBgkqhkiG9w0BAQEFAASCCSwwggkoAgEAAoICAQC6nNHeZ5yKWMJt\n" +
                                          "mq6VjrCIFA9XoUrJDGNV1ILx2jN9FtZJCT3dXSUZofb5ScJqAOdt0y1vVuZLtjG6\n" +
                                          "lTdJUhuBSXiuErdFPvQwFhUy9HQIV1eQVtIFy6bw0j6HiWfx4TmMk3ijx+cFvjDe\n" +
                                          "NIb7xXVdqWiLZEur2Bb4PbyJg3B2vVAeRRxAnvnKUeAdjOsUMo0MMyU+Zbz8KZUW\n" +
                                          "GAgFWK3/+rJfnCcuJ3EykJMAMPKy9sJClv8tH/foTedycJGUCqfGfPK3ibyGILCJ\n" +
                                          "XfNVFvPtrxiJvFoyMBCpz8B84rGTzywt6U8GA9nCFWWhbK700J+UVbr5zRpRMo8f\n" +
                                          "yES/wIF7F/RIJA9KMGTZNp3bf+Y91sHKhIZQUABrCUBMmvMGxwPA8oI2z++aMzng\n" +
                                          "Du/xGPQ8SNwkOhIjrLtbzLP7pINAm8C0xYTfFbTd4SJ9/RAskAqaj5nq1bETk0zK\n" +
                                          "fDSbvTMP+Dw+1CxXE+D+3h/wj5O4jePMPzcNcsb6LFbbG+C2TXGA9d5oIpS4nkPn\n" +
                                          "zPTcgF9hZBvF8TcJUrd3JdjSdTAwrKjNOMPz1sYt9HNwmjx5LWVMXTqWOMwzuDQY\n" +
                                          "HXcvrdTNWcg/0UPebjEmwYOmU8Umk+bij5IaPf5U/1xkKxek8BcxK0NC/oxNU0MH\n" +
                                          "zna2Y+95Ishcyiw2ybB//g+AgeEuRQIDAQABAoICAFCyR77ZJ0RcJZen8B8UWRo/\n" +
                                          "MnM+eEyAYuRWxVk4dlN9cKScrnfvM3/mHhqm3r9gLLO9QkSHW4cZ/l8k74dxThuW\n" +
                                          "Xe1IgqAbHRU+N2SVeSeyPVdZc/C1pDc3c3rA3IYoUu6LRvvsEebV2+P66vQs6xfZ\n" +
                                          "ji/Y6zAgfa5TZBhDEnsGWoL8d78p//KRfUzKB03wnjXMWCqnbBXPFX08XxuQwY2U\n" +
                                          "J1ZY/EhlHfozsp2+jPT+5/pLuYUV8eou5gsCyrEt+mdG2N6tXSzTLgP48KS3DDyY\n" +
                                          "HxBQtuBFEK+d+ysGDKxSRlSEM3vyZlQbVJt9mBqdrKREflpPazX5mxKvU6tn35VR\n" +
                                          "0jWUBGmKgXX0/KWXDuucHq/SM+fkSV2s1BIHQH7jexgQtYoOV8w5D4Agt2pa0Bju\n" +
                                          "nSKymxS6+mBhjTVwq9dFhiTocOnN97JHviVikthxe4GjpAR17SbLBrRHNUPjdqyz\n" +
                                          "FX9FbHeVRRCypN4uebdN0qx1TvLy0MUdr0dcCL4Rsl1GrsqiWGXODibThlRuYM2F\n" +
                                          "IKHhIF0WSYsiHeElBGooqDUB+mLXwgaHlj2ejiKgfuueXfY7K4eeaYzdBa4A45SO\n" +
                                          "9Fq8yrLzwiC4WLLqllCRGTCCJParO8iWk1ku643RPhEpG4xmJ5rmLy0GQZelYqNW\n" +
                                          "CH1He2Ai11/OCg+CSjLxAoIBAQDpV5ShtfsQfIFkhNCzNL0sD6kuhkzi3g1JFq+x\n" +
                                          "v0uqxHmcO4VxpKddgQ7ShmJDPlP8YwAy93IHQT0fg/A3ZHYAaHfrQEpcahnzWnvv\n" +
                                          "sqbO0eQhu+65LYNFz2lF+vMK5BlxWtNZ6XZELAaIE6h5uaxG/GWVuEaVeu44IMNz\n" +
                                          "xYoUNqmNMxZjuQyw+6RfCU5E9Xes+y9QqIXHRMvJB0GbVHmujChfqrCYwO4mSadu\n" +
                                          "XIkIvCfgygSE2zl6fgJanmhm4g8e+ndaz9+XtHjS4nzrehKmjUTzzGWvtUVlzPHt\n" +
                                          "R1T78FFJMgp2SSqWnwcVYDULbooWSpZySgO2qA99CcK8se3XAoIBAQDMu6LeN13N\n" +
                                          "5TtA9ymfmF3FcwsItvhnczaioFrwVWMgozCKAO6KwV5JIdgip2OsGYcKjGx54bf8\n" +
                                          "tZmZu+MfD+fA6O1WXf+BhplMHq4cCHmceZ1lnSlwuYDWGAirY7m+m0YTPHPxP538\n" +
                                          "gW1WGykpZoQGWmQGSg6SZYVWapVw0n2uQnmG1uIoVo61o2sNQToAG94G7Mh5cN10\n" +
                                          "etvRzpwP5g5jTFLzoCN2V++vQ0jbaNWGmjf8cYoRlNBhwtisdStgqQ40XS2o9B/O\n" +
                                          "BbCxJ7C7D+ZxAGz63DVnNC2IU1wbluQN6l3imlSqC9fak2zRq64NEvm87cGS9GCw\n" +
                                          "nKr9ck5Ve6lDAoIBAQCzYEw18/tItS2S5Da7THOQB4n65er9C84SvYnajj/QyrDh\n" +
                                          "1S/EKNswGiIW3I91Odi+UCy7AVV7Fj+ylm53Zpb/wU9OXWHMoRPJ62kS3rc0jk1d\n" +
                                          "UWc2mzIq3EMFNODZIngcl8GZgMsVpPJwcQw2ZvF0sFo3Oi2og5PVOfqGaPi4iTzV\n" +
                                          "+svfZUXIV+oH1NOCV2DkbUP2MVaF1PEDXLymd84CzGPNBx4yjz2D2WMIh8IgIORW\n" +
                                          "/XVz3ELXUz7kAponXOha3Bgci2hK6sVm+A0nx/PGbRrwf9Q7upHarC3eRdOiyR7z\n" +
                                          "sbAxNhWzLBT57bdZTmSAtl3pBdTM+WzOpk45WF6JAoIBAF0j0+GSgKtl6QISL656\n" +
                                          "i9aDEvcA/ptZr/ZmS1jhD7rOYwu1htA7TmCo9AI38sFdz7C916/PP3vJRjrJXndI\n" +
                                          "Lh/2F9td4hqUGHOn1X/NXz4PmcqbqDmvOuyrfG40bVFIpKWlr/2F+qLgYtR0gNJj\n" +
                                          "4BZf0veFg4GYjAOXc6sjJ6g6d70fTz4Yus3prY8XKILaKrUtzbpp3WXbatvVSex1\n" +
                                          "tf/4vhg1t0xkjxdF3gRrehDFYkkHgk+jSvXexeIXYAeWgW7TIS2m+j0pG4xMyTsg\n" +
                                          "7a5lCi9KNUVhWIGXouSuTcwC3nlzyqadwrKetTfiCcnK4zDPXWw0nzRZ4fRxpy9+\n" +
                                          "la8CggEAclYhPcUF2zaeF2UWUaXpTFihM1+ElUoRwoeO8t4sQFNz7piBAemcE+Px\n" +
                                          "MtYJsfnxU1RXCXVlDoLLexYxgRFNbyk+F3fUMZxEOMhtxr3ZnrrSZD/OV+FDySmw\n" +
                                          "gaeHN9OX/LOuoOAkRcWXOojoiX2DaJ2dDO6/zyNzksdchHSD1ACDnnKOoelVEMS3\n" +
                                          "k+EvDAzyW9HjYFcrTtAihv0rGQkxVqoA2fUW1BzUgRJ49NUqVmKTnJQeGCWvklb8\n" +
                                          "2plDPZSdC7tQ8qz6do0lf3zE/XZ/JvhwXGaV88WgLm5UZ/HEY7iSKCHaZxDSc7nY\n" +
                                          "fXKpILIZuY/xqwU6FPBX9/2k9qdf4A==\n" +
                                          "-----END PRIVATE KEY-----";

//    private static final String privKey = "-----BEGIN PRIVATE KEY-----\n" +
//                                          "MIIJnzBJBgkqhkiG9w0BBQ0wPDAbBgkqhkiG9w0BBQwwDgQI+zTtq5vJF0UCAggA" +
//                                          "MB0GCWCGSAFlAwQBKgQQc6I/XzDUMRvKBpetEYxeFQSCCVAGX3yGCH+sXH8ll3gT" +
//                                          "n/G4H4+HFCTnadWAwgSB3Avm1gt8h6ahw8fRSWO1vYT4rWSUBpZgp5LFtHP6xM7e" +
//                                          "j+TjegxvtHqhwq9TfB+XAWE9P1Pf05KgAj2/HDXCTkYL4SdCn1JrgSKNJhGXWxCU" +
//                                          "9uLGhCSA01tK8dEUbbUGAG1coZNkhd5O+D3nULaGvJvOGJmNl2fRnjhAYcGIe5tp" +
//                                          "aCEJF9z9xMgr6g3Rnhfrg33iohnuCosJiBLeir84ea2XahqHKyDAURBWr3EbQr2W" +
//                                          "QmHr48Hw2jogpGxtuTiZf8I3ZXxC91qX/6v6Lg3xwkDDK+mwTbj7hwAVM45GQHpr" +
//                                          "H1MkufZjw51UnKx6822hUojAHXkrxmigknrPc/Ftm5cDKOPfNQtVl0flUmDpY48d" +
//                                          "Tyr76JEw0l4QEFJga7Bwtn2SUmHHOjKAnlNnOvS04D0Hb9qOKxn/DeuRqRHuR6uH" +
//                                          "Oo8HG96RXb5hBOu7hqcAfw/dGkUku4mh9itc0jgImspV73Gy9yY41AbuXW3wVyqb" +
//                                          "iBVpSmKvNIoz4BnOAyjuY/SNJ6aVrbUOCxCfwC9oUjz5QvTpPTwxCDpzStCAeKo0" +
//                                          "nHRWzB7n+d/jACOdFQeENAwuFH4I03pDOEW5AOVKYVkRPtzwclnza6sQI5IKCKAS" +
//                                          "v8ek2KeNNwJvZ5ZitK2U2fVSnVsiOhUphtlpi5Y8Tog8ZJxPT9gNyAqTSiBNmtCF" +
//                                          "TdgMegdWC4Rb11WIyPKSAzSjjBkVYDRqmwF4bmoNKtuPPbxeRUqz4v2mXr7IDTmQ" +
//                                          "rPNnneMfbRfdcAsYnQxEyKKgFMGD2wZsvLbwqhOs0f8/Ll99BsNhSabpHCuRnzwZ" +
//                                          "iLm9YH7mml70V0iaEM6R6fVLhctCFcIJoS31zWN0PXNphcBt+9qDxeOpAz3CnWVw" +
//                                          "IgfbhGf2Z4I+fb2+2EtfQVkV3IRhhima6atnV2tWizcGAWQeV5TtJg1hwRssKS24" +
//                                          "VMmZNMN5FTLk1+K23u2yA0NcSVPJZP4/JfVmB3kpES8aidKhgVrgW/j2YgHm23PW" +
//                                          "PoP0y7ihb5LXlee+KjPnzNNR0fmw6ZQYidetz9QDxjAak5VbRyjOtKkPbppgF1yH" +
//                                          "XYTM4EP7ON8mCNnqBfPsBBknk/e6Bo3QiZcvmloPILEomBJnFVIzITMtvSAxnI2G" +
//                                          "Bv382clw54UZhZGqth83sZbALuQFyImHzRpQoppRnzmeOmkMqM19qLKVF5xR6/0P" +
//                                          "I/YhQOSQiuJ/05PHDiD4OmgGZo7Px1cQ7UfLqwVgaPNhNE1/eQIwT/XtKlMZ7Rim" +
//                                          "ZHRvyaS6ULlVztRxpguyPEq3KhIW+3aoz0ICtH0VPqyJ/h1ebU+Y9clFGk6Lu5hR" +
//                                          "d1q0uhsH2o1uf3xvaW8tEokkgWwCPdQzrXfLiKvZVL7qEuTIWEgbzwdHVUTsG6V3" +
//                                          "yZQ3PJhzILKFynN/+h2pocLI5Lbq/2OO27rWBk3DRVlWniPS/nHZqFXm3Uqg0vdB" +
//                                          "de4f6l3GaV9U8Qoc7Wq/xu/E+zLRfWfro+z2DA5ykKnGQCjqbwSKYBwNTY2XgnO3" +
//                                          "uP6NHFm5hsgoGYk2UvZ2lZKRXySF/bZufaP82H+bPCnkb40BP/x0PoNMFJDfZIqX" +
//                                          "4x1VDUAKcBtB2GounO2kNGphX/VXR7vcvlIr7q032+7Z4ydRkJvbxxy34CxyPZgp" +
//                                          "vuNWSbYJ+1eB8DbWnksq4mHRiuui9SMDbqESpdb6sd+Y/sEDU0a3X7QSsNYeMWbD" +
//                                          "N9IdgqNiX0MkNcmeK96yA5k7MLKK8mnmBF9nmIEXxi0og19/J+WVfTB5WqYzofA/" +
//                                          "TPJ2mdMff3bGx+bgcAC2iFZ55Dtdze32lmKbQpb+eFq/jSXeinzoSbcFyFuf3vLg" +
//                                          "8vs7ml8iLhm1Wun9qzkA+gBJP6J4sD4z8JV3IUM1rp2FybSolzYqoqeElJqupwZh" +
//                                          "kZTRYRCXE1q5GGSwOuW4L2bAXR4UKP4JuMLnDhZ3MCJ/ikRP+K4pj9pxZAd/9mog" +
//                                          "kpWuj131UhqD0sxqEoXiijEPX1qarmzTdpLjdcU0H1PunHuPxfVMbq7/DQYDWPKe" +
//                                          "duskXK/+TWT7XiNfNBBvh+56TEaDXTaf82XzvGj9/uF2MeLwOx8XIgxHLX4GCdAY" +
//                                          "FDQKDA7ZO4dFkUFl05L04k8JldqpbAkux1ruyOH1dGRYC3NojmZ38qvVykHEUgTv" +
//                                          "bdpuPZEijs3o9nZ+85I+xf0RCQw4UmkynngzKRDa6eboexDqokIjewziyXKpNXuY" +
//                                          "jwEGkmM+ie12BMF47dpuEAUDhDqCam/yJ/qIRj5HFynS/A1DXufVvQw/mBgkz4u7" +
//                                          "4XfHNli6ganH3LjnxIO5m646sdfTSHsm3A4FO4MceR9cT1WYdQKf6rI61yVzYZ6d" +
//                                          "qCa/PlbbFYh+eYppqUG4JQHAH9NcmurNwJ47bwAX64nsYW9kxSpHoACFG6tNzUjl" +
//                                          "VC0CfH6pcHlwRKf5qiUyPSmNav2EN1iZkHdlquT1iso0+UtHRFLEpep72TcawVpK" +
//                                          "IHDGWUP+++RQaZNIBAHa9/2NWczbXLgFz4ioUB2PBfEn/QVEhfN/KU/VOF5rViLJ" +
//                                          "umhNAxR0woPbkMWhh095+0Ti6WdQNuenjdLXeWVbit+2JKvXF6funZvb8tIWyIMR" +
//                                          "JBcN2JZG+lUhQiIj6hPttGUYQSHb6sKeSWe6QxIyqDvhkX/uuPKDHOa4lT9ohlsB" +
//                                          "PhTzO9xaUuehoevAJiOSjTxRMLZyd5k4UBzJzCJTHPxQi5MO2QqsZz7HWwtAvTM7" +
//                                          "au2t9kEDcMJ1U9580RSN6B8TAu+GbcODfkIiH7VTudqhqjMK0QWOgN7xrxQNKu5M" +
//                                          "A/D+LDp5NUiwz7QiNgXubIywMgZilxGQe9k3U9xvlyAy18xWjSgrJuxhDt9KYFTQ" +
//                                          "QTxicrmH7uZ+GHvZyN64pKY5k7/bg7DJ6oHs52zM515b3/QDRp6raG47zHKAL1il" +
//                                          "nrJjrUO8RejezSIHgNFi9ZSx6i364EF8Cvouyv5eiUE9LMF+hbScqDChdBZCTLjo" +
//                                          "K6OK+28UvMCoZiOhkr5iaMZFnzA6E/hUJO78Bfd2TnfG/Hog6C+yL2SbnWtQ6+0O" +
//                                          "0PIm9JnKdE6s8jzx+y8J971W9wZVjjDySAhpOcNOvYxLvSnfGQvtgRpO5SqiyfrK" +
//                                          "645qvlfaAzgvG9xtgAHQ38Watg==\n" +
//                                          "-----END PRIVATE KEY-----";

    public JWTTokenBuilder() {
        _claims = new JwtClaims();
        _jws = new JsonWebSignature();
        try {
            _rsajwk = RsaJwkGenerator.generateJwk(2048); // this generates new pub and private key pair but we will replace them.
            _rsajwk.setKeyId("keyid");
            _jws.setKeyIdHeaderValue(_rsajwk.getKeyId());
//            _jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
//            // The JWT is signed using the private key
//            _jws.setKey(this.fromPemEncoded(privKey)); // replace the private key so we can use same public key every time.
//            //_jws.setHeader("typ","JWT");  // not sure if we should do this or let twas figure it out.
//            System.out.println("jws key: " + _jws.getKey());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    private static String pemEncode(Key publicKey) {
        byte[] encoded = publicKey.getEncoded(); // X509 SPKI
        return BEGIN_PUBLIC_KEY + "\r\n" + SimplePEMEncoder.encode(encoded) + "\r\n" + END_PUBLIC_KEY;
    }

    public String getPublicKeyPem() {
        //return RsaKeyUtil.pemEncode(_rsajwk.getPublicKey());
        return pubKey;
    }

    public String getPrivateKeyPem() {
        //byte[] encoded = _rsajwk.getPrivateKey().getEncoded(); // X509 SPKI
        //return BEGIN_PUBLIC_KEY + "\r\n" + SimplePEMEncoder.encode(encoded) + END_PUBLIC_KEY;
        return privKey;
    }

    private PrivateKey fromPemEncoded(String pem) throws JoseException, InvalidKeySpecException, NoSuchAlgorithmException {
        int beginIndex = pem.indexOf(BEGIN_PRIV_KEY) + BEGIN_PRIV_KEY.length();
        int endIndex = pem.indexOf(END_PRIV_KEY);
        String base64 = pem.substring(beginIndex, endIndex).trim();
        System.out.println("base64: " + base64 + " end");
        byte[] decode = SimplePEMEncoder.decode(base64);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public JWTTokenBuilder setIssuer(String in) {
        _claims.setIssuer(in);
        return this;
    }

    public JWTTokenBuilder setAudience(String in) {
        _claims.setAudience(in);
        return this;
    }

    public JWTTokenBuilder setAudience(String... in) {
        _claims.setAudience(in);
        return this;
    }

    public JWTTokenBuilder setExpirationTimeMinutesIntheFuture(int in) {
        _claims.setExpirationTimeMinutesInTheFuture(in);
        return this;
    }

    public JWTTokenBuilder setExpirationTimeSecondsFromNow(int in) {
        NumericDate exp = NumericDate.now();
        exp.addSeconds(in);
        _claims.setExpirationTime(exp);
        return this;
    }

    public JWTTokenBuilder setExpirationTime(NumericDate exp) {
        _claims.setExpirationTime(exp);
        return this;
    }

    public JWTTokenBuilder setGeneratedJwtId() {
        _claims.setGeneratedJwtId();
        return this;
    }

    public JWTTokenBuilder setJwtId(String in) {
        _claims.setJwtId(in);
        return this;
    }

    public JWTTokenBuilder setIssuedAtToNow() {
        _claims.setIssuedAtToNow();
        return this;
    }

    public JWTTokenBuilder setIssuedAt(NumericDate in) {
        _claims.setIssuedAt(in);
        return this;
    }

    public JWTTokenBuilder setNotBeforeMinutesInThePast(int in) {
        _claims.setNotBeforeMinutesInThePast(in);
        return this;
    }

    public JWTTokenBuilder setNotBefore(NumericDate in) {
        _claims.setNotBefore(in);
        return this;
    }

    public JWTTokenBuilder setSubject(String in) {

        _claims.setSubject(in);
        return this;
    }

    public JWTTokenBuilder setScope(String in) {

        _claims.setClaim(ClaimConstants.SCOPE, in);
        return this;
    }

    public JWTTokenBuilder setRealmName(String in) {

        _claims.setClaim(ClaimConstants.REALM_NAME, in);
        return this;
    }

    public JWTTokenBuilder setTokenType(String in) {

        _claims.setClaim(ClaimConstants.TOKEN_TYPE, in);
        return this;
    }
//    public JWTTokenBuilder setScope(String... in) {
//
//        _claims.setStringListClaim(ClaimConstants.SCOPE, in);
//        return this;
//    }

    public JWTTokenBuilder setClaim(String name, Object val) {
        _claims.setClaim(name, val);
        return this;
    }

    public JWTTokenBuilder unsetClaim(String name) {
        _claims.unsetClaim(name);
        return this;
    }

    // todo: groups?

    public JWTTokenBuilder setKey(Key key) {
        _jws.setKey(key);
        return this;
    }

    public JWTTokenBuilder setKeyIdHeaderValue(String id) {
        _jws.setKeyIdHeaderValue(id);
        return this;
    }

    public JWTTokenBuilder setHSAKey(String keyId) {
        try {
            _jws.setKey(new HmacKey(keyId.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            _jws.setKey(null);
        }
        return this;
    }

    public JWTTokenBuilder setRSAKey() {
        try {
            _jws.setKey(this.fromPemEncoded(privKey));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            _jws.setKey(null);
        }
        return this;
    }

    public JWTTokenBuilder setAlorithmHeaderValue(String alg) {
        _jws.setAlgorithmHeaderValue(alg);
        return this;
    }

    public JWTTokenBuilder setKeyId(String kid) {
        _jws.setKeyIdHeaderValue(kid);
        return this;
    }

    public String build() {
        try {
            if (_claims.getIssuedAt() == null) {
                _claims.setIssuedAtToNow();
            }
        } catch (MalformedClaimException e1) {
            e1.printStackTrace(System.out);
        }
        try {
            _jws.setPayload(_claims.toJson());
            System.out.println("after setPayload");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
        // key may already have been set with setkey
        // kidheadervalue may have already been set
        // algoheadervalue may have already been set
        try {
            System.out.println("jwt: " + _jwt);
            System.out.println("jws: " + _jws);
            System.out.println("jws: " + _jws.getKey());
//            _jws.setKey(this.fromPemEncoded(privKey)); // replace the private key so we can use same public key every time.
            _jwt = _jws.getCompactSerialization();
            System.out.println("after compact");
            return _jwt;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }

    }

    public String getJsonClaims() {
        return _claims.toJson();
    }

    public String getJwt() {
        return _jwt;
    }

    public JwtClaims getRawClaims() {
        return _claims;
    }
}
