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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.release.plugin.slsa.v1_2.DsseEnvelope;
import org.apache.commons.release.plugin.slsa.v1_2.Statement;
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
     * Not instantiable.
     */
    private DsseUtils() {
    }

    /**
     * Creates and prepares a {@link GpgSigner} from the given configuration.
     *
     * <p>The returned signer has {@link AbstractGpgSigner#prepare()} already called and is ready for use with {@link #signPaeFile(AbstractGpgSigner, Path)}.</p>
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
     * Serializes {@code statement} to JSON, computes the DSSE Pre-Authentication Encoding (PAE), and writes
     * the result to {@code buildDirectory/statement.pae}.
     *
     * <p>The PAE format is:
     * {@code "DSSEv1" SP LEN(payloadType) SP payloadType SP LEN(payload) SP payload},
     * where {@code LEN} is the ASCII decimal byte-length of the operand.</p>
     *
     * @param statement      the attestation statement to encode
     * @param objectMapper   the Jackson mapper used to serialize {@code statement}
     * @param buildDirectory directory in which the PAE file is created
     * @return path to the written PAE file
     * @throws MojoExecutionException if serialization or I/O fails
     */
    public static Path writePaeFile(final Statement statement, final ObjectMapper objectMapper, final Path buildDirectory) throws MojoExecutionException {
        try {
            return writePaeFile(objectMapper.writeValueAsBytes(statement), buildDirectory);
        } catch (final JsonProcessingException e) {
            throw new MojoExecutionException("Failed to serialize attestation statement", e);
        }
    }

    /**
     * Computes the DSSE Pre-Authentication Encoding (PAE) for {@code statementBytes} and writes it to
     * {@code buildDirectory/statement.pae}.
     *
     * <p>Use this overload when the statement has already been serialized to bytes, so the same byte array
     * can be reused as the {@link DsseEnvelope#setPayload(byte[]) envelope payload} without a second
     * serialization pass.</p>
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
     * Signs {@code paeFile} using {@link AbstractGpgSigner#generateSignatureForArtifact(File)},
     * then decodes the resulting ASCII-armored {@code .asc} file with BouncyCastle and returns the raw
     * binary PGP signature bytes.
     *
     * <p>The signer must already have {@link AbstractGpgSigner#prepare()} called before this method is
     * invoked. The {@code .asc} file produced by the signer is not deleted; callers may remove it once
     * the raw bytes have been consumed.</p>
     *
     * @param signer  the configured, prepared signer
     * @param paeFile path to the PAE-encoded file to sign
     * @return raw binary PGP signature bytes (suitable for storing in {@link org.apache.commons.release.plugin.slsa.v1_2.Signature#setSig})
     * @throws MojoExecutionException if signing or signature decoding fails
     */
    public static byte[] signPaeFile(final AbstractGpgSigner signer, final Path paeFile) throws MojoExecutionException {
        final File signatureFile = signer.generateSignatureForArtifact(paeFile.toFile());
        try (InputStream in = Files.newInputStream(signatureFile.toPath()); ArmoredInputStream armoredIn = new ArmoredInputStream(in)) {
            return IOUtils.toByteArray(armoredIn);
        } catch (final IOException e) {
            throw new MojoExecutionException("Failed to read signature file: " + signatureFile, e);
        }
    }

    /**
     * Extracts the key identifier from a binary OpenPGP Signature Packet.
     *
     * <p>Inspects the hashed subpackets for an {@code IssuerFingerprint} subpacket (type&nbsp;33),
     * which carries the full public-key fingerprint and is present in all signatures produced by
     * GPG&nbsp;2.1+. Falls back to the 8-byte {@code IssuerKeyID} from the unhashed subpackets
     * when no fingerprint subpacket is found.</p>
     *
     * @param sigBytes raw binary OpenPGP Signature Packet bytes, as returned by
     *                 {@link #signPaeFile(AbstractGpgSigner, Path)}
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
}
