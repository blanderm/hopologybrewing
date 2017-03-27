package com.hopologybrewing.bcs.capture.aws.lambda;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ddcbryanl on 3/17/17.
 */
public abstract class Poller {
    protected static String DECRYPTED_UNAME = decryptKey("user");
    protected static String DECRYPTED_PASS = decryptKey("pwd");
    protected static String DECRYPTED_IP = decryptKey("bcs_ip");

    private static String decryptKey(String key) {
        System.out.println("Decrypting key " + key);
        byte[] encryptedKey = Base64.decode(System.getenv(key));
        AWSKMS client = AWSKMSClientBuilder.defaultClient();

        DecryptRequest request = new DecryptRequest()
                .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));

        ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
        return new String(plainTextKey.array(), Charset.forName("UTF-8"));
    }
}
