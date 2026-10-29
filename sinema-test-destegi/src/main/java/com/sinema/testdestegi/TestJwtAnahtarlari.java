package com.sinema.testdestegi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Testler için JVM başına bir kez üretilen geçici RSA anahtar çifti. Gerçek anahtarlar (keys/) repoya
 * girmediği ve testte private key commit etmek istemediğimiz için anahtar çalışma anında üretilip
 * geçici klasöre PEM olarak yazılır. Testler bu private key ile kendi token'larını da imzalayabilir.
 */
public final class TestJwtAnahtarlari {

    private static final int ANAHTAR_BITI = 2048; // testte hız için; üretimdeki 3072
    private static final KeyPair ANAHTAR_CIFTI = anahtarCiftiUret();
    private static final Path OZEL_ANAHTAR_DOSYASI = pemYaz("jwt-private.pem", "PRIVATE KEY",
            ANAHTAR_CIFTI.getPrivate().getEncoded());
    private static final Path ACIK_ANAHTAR_DOSYASI = pemYaz("jwt-public.pem", "PUBLIC KEY",
            ANAHTAR_CIFTI.getPublic().getEncoded());

    private TestJwtAnahtarlari() {
    }

    public static RSAPrivateKey ozelAnahtar() {
        return (RSAPrivateKey) ANAHTAR_CIFTI.getPrivate();
    }

    public static RSAPublicKey acikAnahtar() {
        return (RSAPublicKey) ANAHTAR_CIFTI.getPublic();
    }

    /** Spring Resource biçiminde ("file:///...") konum. */
    public static String ozelAnahtarKonumu() {
        return OZEL_ANAHTAR_DOSYASI.toUri().toString();
    }

    public static String acikAnahtarKonumu() {
        return ACIK_ANAHTAR_DOSYASI.toUri().toString();
    }

    private static KeyPair anahtarCiftiUret() {
        try {
            KeyPairGenerator uretici = KeyPairGenerator.getInstance("RSA");
            uretici.initialize(ANAHTAR_BITI);
            return uretici.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA desteklenmiyor", e);
        }
    }

    // getEncoded(): private için PKCS#8, public için X.509 DER; PEM = başlık + Base64 (64 sütun) + bitiş
    private static Path pemYaz(String dosyaAdi, String tur, byte[] der) {
        String pem = "-----BEGIN " + tur + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der)
                + "\n-----END " + tur + "-----\n";
        try {
            Path klasor = Files.createTempDirectory("sinema-jwt-test");
            Path dosya = Files.writeString(klasor.resolve(dosyaAdi), pem, StandardCharsets.US_ASCII);
            dosya.toFile().deleteOnExit();
            klasor.toFile().deleteOnExit();
            return dosya;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
