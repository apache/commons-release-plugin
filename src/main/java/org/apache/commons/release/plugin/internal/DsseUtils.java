/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.release.plugin.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.release.plugin.slsa.v1_2.DsseEnvelope;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.gpg.AbstractGpgSigner;
import org.apache.maven.plugins.gpg.GpgSigner;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.sig.IssuerFingerprint;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;

/**
 * Utility methods for creating DSSE (Dead Simple Signing Envelope) envelopes signed with a PGP key.
 */
public final class DsseUtils {

    /**
     * Creates and prepares a {@link GpgSigner} from the given configuration.
     *
     * <p>The returned signer has {@link AbstractGpgSigner#prepare()} already called and is ready for use with {@link #signFile(AbstractGpgSigner, Path)}.</p>
     *
     * @param executable     path to the GPG executable, or {@code null} to use {@code gpg} from {@code PATH}
     * @param defaultKeyring whether to include the default GPG keyring
     * @param lockMode       GPG lock mode ({@code "once"}, {@code "multiple"}, or {@code "never"}), or {@code null} for no explicit lock flag
     * @param keyname        name or fingerprint of the signing key, or {@code null} for the default key
     * @param useAgent       whether to use gpg-agent for passphrase management
     * @param log            Maven logger to attach to the signer
     * @return a prepared {@link AbstractGpgSigner}
     * @throws MojoFailureException if {@link AbstractGpgSigner#prepare()} fails
     */
    public static AbstractGpgSigner createGpgSigner(final String executable, final boolean defaultKeyring, final String lockMode, final String keyname,
            final boolean useAgent, final Log log) throws MojoFailureException {
        final GpgSigner signer = new GpgSigner(executable);
        signer.setDefaultKeyring(defaultKeyring);
        signer.setLockMode(lockMode);
        signer.setKeyName(keyname);
        signer.setUseAgent(useAgent);
        signer.setLog(log);
        signer.prepare();
        return signer;
    }

    /**
     * Extracts the key identifier from a binary OpenPGP Signature Packet.
     *
     * @param sigBytes raw binary OpenPGP Signature Packet bytes
     * @return uppercase hex-encoded fingerprint or key ID string
     * @throws MojoExecutionException if {@code sigBytes} cannot be parsed as an OpenPGP signature
     */
    public static String getKeyId(final byte[] sigBytes) throws MojoExecutionException {
        try {
            final PGPSignatureList sigList = (PGPSignatureList) new BcPGPObjectFactory(sigBytes).nextObject();
            final PGPSignature sig = sigList.get(0);
            final PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
            if (hashed != null) {
                final IssuerFingerprint fp = hashed.getIssuerFingerprint();
                if (fp != null) {
                    return Hex.encodeHexString(fp.getFingerprint());
                }
            }
            return Long.toHexString(sig.getKeyID()).toUpperCase(Locale.ROOT);
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to extract key ID from signature", e);
        }
    }

    /**
     * Signs {@code paeFile} and returns the raw OpenPGP signature bytes.
     *
     * <p>The signer must already have {@link AbstractGpgSigner#prepare()} called before this method is invoked.</p>
     *
     * @param signer  the configured, prepared signer
     * @param path path to the file to sign
     * @return raw binary PGP signature bytes
     * @throws MojoExecutionException if signing or signature decoding fails
     */
    public static byte[] signFile(final AbstractGpgSigner signer, final Path path) throws MojoExecutionException {
        final Path signaturePath = signer.generateSignatureForArtifact(path.toFile()).toPath();
        final byte[] signatureBytes;
        try (InputStream in = Files.newInputStream(signaturePath); ArmoredInputStream armoredIn = new ArmoredInputStream(in)) {
            signatureBytes = IOUtils.toByteArray(armoredIn);
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to read signature file: " + signaturePath, e);
        }
        try {
            Files.delete(signaturePath);
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to delete signature file: " + signaturePath, e);
        }
        return signatureBytes;
    }

    /**
     * Writes serialized JSON to a file using the DSSE Pre-Authentication Encoding (PAE).
     *
     * <pre>PAE(type, body) = "DSSEv1" + SP + LEN(type) + SP + type + SP + LEN(body) + SP + body</pre>
     *
     * @param statementBytes the already-serialized JSON statement bytes to encode
     * @param buildDirectory directory in which the PAE file is created
     * @return path to the written PAE file
     * @throws MojoExecutionException if I/O fails
     */
    public static Path writePaeFile(final byte[] statementBytes, final Path buildDirectory) throws MojoExecutionException {
        try {
            final byte[] payloadTypeBytes = DsseEnvelope.PAYLOAD_TYPE.getBytes(StandardCharsets.UTF_8);

            final ByteArrayOutputStream pae = new ByteArrayOutputStream();
            pae.write(("DSSEv1 " + payloadTypeBytes.length + " ").getBytes(StandardCharsets.UTF_8));
            pae.write(payloadTypeBytes);
            pae.write((" " + statementBytes.length + " ").getBytes(StandardCharsets.UTF_8));
            pae.write(statementBytes);

            final Path paeFile = buildDirectory.resolve("statement.pae");
            Files.write(paeFile, pae.toByteArray());
            return paeFile;
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to write PAE file", e);
        }
    }

    /**
     * Not instantiable.
     */
    private DsseUtils() {
    }
}
