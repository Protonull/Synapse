package gjum.minecraft.civ.synapse.common.network;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.jetbrains.annotations.NotNull;

public final class CryptoTransformer {
    public enum Type { ENCRYPT, DECRYPT }

    private final Cipher cipher;

    public CryptoTransformer(
        final @NotNull Key key,
        final @NotNull Type type
    ) throws InvalidKeyException {
        try {
            this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        }
        catch (final NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Something impossible just happened! RSA/ECB/PKCS1Padding should exist!", e);
        }
        this.cipher.init(
            switch (type) {
                case ENCRYPT -> Cipher.ENCRYPT_MODE;
                case DECRYPT -> Cipher.DECRYPT_MODE;
            },
            key
        );
    }

    public byte @NotNull [] transform(
        final byte @NotNull [] data
    ) throws IllegalBlockSizeException, BadPaddingException {
        synchronized (this.cipher) {
            return this.cipher.doFinal(data);
        }
    }
}
