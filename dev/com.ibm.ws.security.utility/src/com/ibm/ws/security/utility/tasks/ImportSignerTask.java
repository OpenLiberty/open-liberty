package com.ibm.ws.security.utility.tasks;

import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.CertCapture;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImportSignerTask extends BaseCommandTask {
    private static final String ARG_HOST = "--host";
    private static final String ARG_PORT = "--port";
    public static final String ARG_KEYSTORE = "--keyStore";
    public static final String ARG_PASSWORD = "--password";
    public static final String ARG_ALIAS = "--alias";
    public static final String ARG_TYPE = "--type";
    public static final List<String> KNOWN_ARGS = Arrays.asList(ARG_HOST, ARG_PORT, ARG_PASSWORD, ARG_KEYSTORE, ARG_ALIAS, ARG_TYPE);
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
        return KNOWN_ARGS.contains(arg);
    }

    @Override
    void checkRequiredArguments(String[] args) throws IllegalArgumentException {
        StringBuilder message = new StringBuilder();
        // We expect at least two arguments and the task name
        if (args.length < 4) {
            message.append(getMessage("insufficientArgs"));
        }

        List<String> argList = new ArrayList<>(args.length);
        for (String arg: args) {
            if (arg.startsWith("--")) {
                int index = arg.indexOf('=');
                if (index != -1) {
                    arg = arg.substring(0, index);
                }
                argList.add(arg);
            }
        }

        if (!argList.contains(ARG_HOST)) {
            message.append(" ").append(getMessage("missingArg", ARG_HOST));
        }

        if (!argList.contains(ARG_PORT)) {
            message.append(" ").append(getMessage("missingArg", ARG_PORT));
        } else {
            // check number
            String port = getArgumentValue(ARG_PORT, args, null, null, null, null);
            try {
                Integer.parseInt(port);
            } catch (NumberFormatException nfe) {
                message.append(" ").append(getMessage("importSigner.invalidPort", port));
            }
        }

        if (!argList.contains(ARG_KEYSTORE)) {
            message.append(" ").append(getMessage("missingArg", ARG_KEYSTORE));
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

        validateArgumentList(args, Collections.singletonList(ARG_PASSWORD));

        String host = getArgumentValue(ARG_HOST, args, null, null, stdin, stderr);
        int port = Integer.parseInt(getArgumentValue(ARG_PORT, args, null, null, stdin, stderr));
        String keyStore = getArgumentValue(ARG_KEYSTORE, args, null, null, stdin, stderr);
        String keyStorePassword = getArgumentValue(ARG_PASSWORD, args,null, ARG_PASSWORD, stdin, stderr);
        String alias = getArgumentValue(ARG_ALIAS, args,null, null, stdin, stderr);
        String type = getArgumentValue(ARG_TYPE, args, null, null, stdin, stderr);

        if (type == null) {
            if (keyStore.endsWith(".p12")) {
                type = "PKCS12";
            } else if (keyStore.endsWith(".jks")) {
                type = "JKS";
            } else {
                throw new IllegalArgumentException(getMessage("missingArg", ARG_TYPE));
            }
        }

        X509Certificate[] certs = CertCapture.retrieveCertificates(host, port);

        if (certs == null || certs.length == 0) {
            stderr.println(getMessage("importSigner.selfSigned", host));
            return SecurityUtilityReturnCodes.ERR_CERT_CHAIN_NOT_FOUND;
        }

        printCerts(certs);

        if (stdin.confirm("importSigner.confirm")) {
            if (!storeCert(certs[1], new File(keyStore), type, keyStorePassword.toCharArray(), alias, stderr)) {
                return SecurityUtilityReturnCodes.ERR_WRITE_FAILED;
            }
        }

        return SecurityUtilityReturnCodes.OK;
    }

    private boolean storeCert(X509Certificate cert, File keyStore, String type, char[] pwd, String alias, PrintStream stderr) {
        try {
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

            return true;
        } catch (Exception e) {
            stderr.println(getMessage("importSigner.failWritingKeystore", keyStore, e.getMessage()));
            return false;
        }
    }

    private static void printCerts(X509Certificate[] chain) throws CertificateEncodingException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            X509Certificate cert = chain[i];
            builder.append("Subject DN:\t").append(cert.getSubjectDN()).append("\r\n")
                    .append("Issuer DN:\t").append(cert.getIssuerDN()).append("\r\n")
                    .append("Serial number:\t").append(toHex(cert.getSerialNumber().toByteArray())).append("\r\n")
                    .append("Expires:\t").append(cert.getNotAfter()).append("\r\n")
                    .append("SHA-1 Digest:\t").append(hash(cert.getEncoded(), sha1Hasher)).append("\r\n")
                    .append("SHA-256 Digest:\t").append(hash(cert.getEncoded(), sha256Hasher)).append("\r\n")
                    .append("\r\n");
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
