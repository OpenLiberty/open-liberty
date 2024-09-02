package com.ibm.ws.security.utility.tasks;

import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.CertCapture;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class ImportSignerTask extends BaseCommandTask {
    private static final String ARG_HOST = "--host";
    private static final String ARG_PORT = "--port";
    public static final String ARG_KEYSTORE = "--keystore";
    public static final String ARG_PASSWORD = "--password";
    public static final String ARG_ALIAS = "--alias";
    public static final String ARG_TYPE = "--type";
    private static final String ARG_ACCEPT = "--accept";
    public static final List<String> KNOWN_ARGS = Arrays.asList(ARG_HOST, ARG_PORT, ARG_PASSWORD, ARG_KEYSTORE, ARG_ALIAS, ARG_TYPE, ARG_ACCEPT);
    private static final String BEGIN_PEM = "-----BEGIN CERTIFICATE-----";
    private static final String END_PEM = "-----END CERTIFICATE-----";
    private static MessageDigest sha1Hasher;
    private static MessageDigest sha256Hasher;

    static {
        try {
            sha1Hasher = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ignored) {
        }
        try {
            sha256Hasher = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ignored) {
        }
    }
    public ImportSignerTask(String scriptName) {
        super(scriptName);
    }

    @Override
    boolean isKnownArgument(String arg) {
        return KNOWN_ARGS.contains(arg.toLowerCase());
    }

    @Override
    void checkRequiredArguments(String[] args) throws IllegalArgumentException {
        StringBuilder message = new StringBuilder();
        // We expect at least two arguments and the task name
        if (args.length < 3) {
            message.append(getMessage("insufficientArgs"));
        }

        List<String> argList = new ArrayList<>(args.length);
        for (String arg: args) {
            if (arg.startsWith("--")) {
                int index = arg.indexOf('=');
                if (index != -1) {
                    arg = arg.substring(0, index);
                }
                argList.add(arg.toLowerCase());
            }
        }

        if (!argList.contains(ARG_HOST)) {
            message.append(" ").append(getMessage("missingArg", ARG_HOST));
        }

        // Port is not required, but checking the value here anyway
        if (argList.contains(ARG_PORT)) {
            // check port is a valid number
            String port = getArgumentValue(ARG_PORT, args, "443", null, null, null);
            try {
                Integer.parseInt(port);
            } catch (NumberFormatException nfe) {
                message.append(" ").append(getMessage("importSigner.invalidPort", port));
            }
        }

        if (!argList.contains(ARG_KEYSTORE)) {
            message.append(" ").append(getMessage("missingArg", ARG_KEYSTORE));
        } else {
            String keyStore = getArgumentValue(ARG_KEYSTORE, args, null, null, null, null);
            File f = new File(keyStore);
            String simpleName = f.getName().toLowerCase();
            if (!simpleName.endsWith(".pem")) {
                if (!argList.contains(ARG_PASSWORD)) {
                    message.append(" ").append(getMessage("missingArg", ARG_PASSWORD));
                }
            }
        }

        if (!argList.contains(ARG_HOST)) {
            message.append(" ").append(getMessage("missingArg", ARG_PASSWORD));
        }

        String msg = message.toString();

        if (!msg.isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
    }

    @Override
    public String getTaskName() {
        return "importSigner";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("importSigner.desc", "importSigner.usage.options",
                "importSigner.required-key.", "importSigner.required-desc.",
                "importSigner.option-key.", "importSigner.option-desc.",
                null, null,
                scriptName);
    }

    @Override
    public String getTaskDescription() {
        return getOption("importSigner.desc", true);
    }

    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {

        validateArgumentList(args, Arrays.asList(ARG_PASSWORD, ARG_ACCEPT));

        String host = getArgumentValue(ARG_HOST, args, null, null, stdin, stderr);
        int port = Integer.parseInt(getArgumentValue(ARG_PORT, args, "443", null, stdin, stderr));
        String keyStore = getArgumentValue(ARG_KEYSTORE, args, null, null, stdin, stderr);
        String keyStorePassword = getArgumentValue(ARG_PASSWORD, args,"", ARG_PASSWORD, stdin, stderr);
        String alias = getArgumentValue(ARG_ALIAS, args,null, null, stdin, stderr);
        String type = getArgumentValue(ARG_TYPE, args, null, null, stdin, stderr);
        String accept = getArgumentValue(ARG_ACCEPT, args, "false", null, stdin, stderr);

        File ksFile = new File(keyStore);

        if (type == null) {
            String simpleName = ksFile.getName().toLowerCase();
            if (simpleName.endsWith(".p12")) {
                type = "PKCS12";
            } else if (simpleName.endsWith(".jks")) {
                type = "JKS";
            } else if (simpleName.endsWith(".pem")) {
                type = "PEM";
            } else {
                throw new IllegalArgumentException(getMessage("missingArg", ARG_TYPE));
            }
        }

        X509Certificate[] certs = CertCapture.retrieveCertificates(host, port);

        if (certs != null) {
            printCerts(certs);

            boolean importCert = false;
            if ("false".equals(accept)) {
                // default for accept if not specified is false which means prompt
                importCert = stdin.confirm("importSigner.confirm");
            } else if (accept == null) {
                // If accept has no argument do an unconditional import
                importCert = true;
                stdout.println(getMessage("importSigner.alwaysImport"));
            } else {
                importCert = isAccept(certs, accept);
                if (!importCert) stdout.println(getMessage("importSigner.nomatch"));
            }

            if (importCert) {
                X509Certificate certToStore;

                if (certs.length == 1) {
                    certToStore = certs[0];
                } else {
                    certToStore = certs[1];
                }

                if (!storeCert(certToStore, ksFile, type, keyStorePassword.toCharArray(), alias, stderr)) {
                    return SecurityUtilityReturnCodes.ERR_WRITE_FAILED;
                }
                stdout.println(getMessage("importSigner.imported", keyStore));
            }
        } else {
            stderr.println(getMessage("importSigner.noCertsFound", host, port));
            return SecurityUtilityReturnCodes.ERR_CERT_CHAIN_NOT_FOUND;
        }

        return SecurityUtilityReturnCodes.OK;
    }

    private boolean isAccept(X509Certificate[] certs, String accept) throws CertificateEncodingException {

        X509Certificate cert = certs[0];
        int len = accept.length();
        if (len == 95) {
            // check sha256
            CharSequence shaHash = hash(cert.getEncoded(), sha256Hasher);
            String str = new StringBuilder().append(shaHash).toString();
            return str.equalsIgnoreCase(accept);
        } else if (len == 59) {
            // check sha1
            CharSequence shaHash = hash(cert.getEncoded(), sha1Hasher);
            String str = new StringBuilder().append(shaHash).toString();
            return str.equalsIgnoreCase(accept);
        }

        return false;
    }

    private boolean storeCert(X509Certificate cert, File keyStore, String type, char[] pwd, String alias, PrintStream stderr) {
        try {
            if ("PEM".equalsIgnoreCase(type)) {
                PrintStream pemFile = new PrintStream(keyStore);
                pemFile.println(BEGIN_PEM);
                Base64.Encoder enc = Base64.getMimeEncoder(64, System.getProperty("line.separator").getBytes(StandardCharsets.UTF_8));
                pemFile.println(enc.encodeToString(cert.getEncoded()));
                pemFile.println(END_PEM);
            } else {
                KeyStore ks = KeyStore.getInstance(type);
                if (keyStore.exists()) {
                    ks.load(new FileInputStream(keyStore), pwd);
                } else {
                    ks.load(null, pwd);
                }
                if (alias == null) {
                    alias = cert.getSubjectDN().getName();
                }
                ks.setCertificateEntry(alias, cert);
                ks.store(new FileOutputStream(keyStore), pwd);
            }
        } catch (Exception e) {
            stderr.println(getMessage("importSigner.failWritingKeystore", keyStore, e.getMessage()));
            return false;
        }
        return true;
    }

    private static void printCerts(X509Certificate[] chain) throws CertificateEncodingException {
        StringBuilder builder = new StringBuilder();
        int len = (chain.length == 1) ? 1 : 2;
        for (int i = 0; i < len; i++) {
            X509Certificate cert = chain[i];
            builder.append("Subject DN:\t").append(cert.getSubjectX500Principal()).append("\r\n")
                    .append("Issuer DN:\t").append(cert.getIssuerX500Principal()).append("\r\n")
                    .append("Serial number:\t").append(toHex(cert.getSerialNumber().toByteArray())).append("\r\n")
                    .append("Expires:\t").append(cert.getNotAfter()).append("\r\n");
            if (sha1Hasher != null) {
                builder.append("SHA-1 Digest:\t").append(hash(cert.getEncoded(), sha1Hasher)).append("\r\n");
            }
            if (sha256Hasher != null) {
                builder.append("SHA-256 Digest:\t").append(hash(cert.getEncoded(), sha256Hasher)).append("\r\n");
            }
            builder.append("\r\n");
        }

        System.out.println(builder);
    }

    private static CharSequence hash(byte[] encoded, MessageDigest hasher) {
        byte[] hash = hasher.digest(encoded);

        return toHex(hash);
    }

    private static CharSequence toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
            builder.append(':');
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder;
    }
}
