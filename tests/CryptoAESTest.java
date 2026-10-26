package util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.BadPaddingException;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CryptoAESTest {

    @TempDir
    Path tempDir;

    @Test
    void encryptsAndDecryptsUsingGeneratedKey() throws Exception {
        File keyFile = tempDir.resolve("aes.key").toFile();
        byte[] original = "texto confidencial".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        CryptoAES crypto = new CryptoAES();

        assertNull(crypto.getTextoCifrado());
        assertNull(crypto.getTextoDecifrado());

        crypto.geraChave(keyFile);
        crypto.geraCifra(original, keyFile);
        byte[] encrypted = crypto.getTextoCifrado();

        assertNotNull(encrypted);
        assertFalse(Arrays.equals(original, encrypted));

        crypto.geraDecifra(encrypted, keyFile);
        assertArrayEquals(original, crypto.getTextoDecifrado());
    }

    @Test
    void eachEncryptionUsesDifferentCiphertext() throws Exception {
        File keyFile = tempDir.resolve("aes.key").toFile();
        CryptoAES crypto = new CryptoAES();
        crypto.geraChave(keyFile);

        crypto.geraCifra(new byte[]{1, 2, 3}, keyFile);
        byte[] first = crypto.getTextoCifrado();
        crypto.geraCifra(new byte[]{1, 2, 3}, keyFile);
        byte[] second = crypto.getTextoCifrado();

        assertFalse(Arrays.equals(first, second));
    }

    @Test
    void rejectsTamperedCiphertext() throws Exception {
        File keyFile = tempDir.resolve("aes.key").toFile();
        CryptoAES crypto = new CryptoAES();
        crypto.geraChave(keyFile);
        crypto.geraCifra(new byte[]{1, 2, 3}, keyFile);
        byte[] tampered = crypto.getTextoCifrado();
        tampered[tampered.length - 1] ^= 1;

        assertThrows(BadPaddingException.class, () -> crypto.geraDecifra(tampered, keyFile));
        assertNull(crypto.getTextoDecifrado());
    }

    @Test
    void rejectsInvalidCiphertextHeader() throws Exception {
        File keyFile = tempDir.resolve("aes.key").toFile();
        CryptoAES crypto = new CryptoAES();
        crypto.geraChave(keyFile);

        assertThrows(IllegalArgumentException.class,
            () -> crypto.geraDecifra(new byte[40], keyFile));
    }

    @Test
    void refusesToOverwriteAnExistingKeyFile() throws Exception {
        File keyFile = tempDir.resolve("aes.key").toFile();
        CryptoAES crypto = new CryptoAES();
        crypto.geraChave(keyFile);

        assertThrows(java.nio.file.FileAlreadyExistsException.class,
            () -> crypto.geraChave(keyFile));
    }
}
