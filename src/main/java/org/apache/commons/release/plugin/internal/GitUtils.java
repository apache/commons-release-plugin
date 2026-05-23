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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.GitIdentifiers;

/**
 * Utilities for Git operations.
 */
public final class GitUtils {

    /**
     * Prefix used in a {@code gitfile} to point to the Git directory.
     *
     * <p>See <a href="https://git-scm.com/docs/gitrepository-layout">gitrepository-layout</a>.</p>
     */
    private static final String GITDIR_PREFIX = "gitdir: ";
    /**
     * Maximum number of symbolic-ref hops before we give up (to avoid cycles).
     */
    private static final int MAX_REF_DEPTH = 5;
    /**
     * Prefix used in {@code HEAD} and ref files to indicate a symbolic reference.
     */
    private static final String REF_PREFIX = "ref: ";
    /**
     * The SCM URI prefix for Git repositories.
     */
    private static final String SCM_GIT_PREFIX = "scm:git:";

    /**
     * Walks up the directory tree from {@code path} to find the {@code .git} directory.
     *
     * @param path A path inside the Git repository.
     * @return The path to the {@code .git} directory (or file for worktrees).
     * @throws IOException If no {@code .git} directory is found.
     */
    private static Path findGitDir(final Path path) throws IOException {
        Path current = path.toAbsolutePath();
        while (current != null) {
            final Path candidate = current.resolve(".git");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            if (Files.isRegularFile(candidate)) {
                // git worktree: .git is a file containing "gitdir: /path/to/real/.git"
                final String content = new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8).trim();
                if (content.startsWith(GITDIR_PREFIX)) {
                    return current.resolve(content.substring(GITDIR_PREFIX.length()));
                }
            }
            current = current.getParent();
        }
        throw new IOException("No .git directory found above: " + path);
    }

    /**
     * Gets the current branch name for the given repository path.
     *
     * <p>Returns the commit SHA if the repository is in a detached HEAD state.
     *
     * @param repositoryPath A path inside the Git repository.
     * @return The current branch name, or the commit SHA for a detached HEAD.
     * @throws IOException If the {@code .git} directory cannot be found or read.
     */
    public static String getCurrentBranch(final Path repositoryPath) throws IOException {
        final Path gitDir = findGitDir(repositoryPath);
        final String head = readHead(gitDir);
        if (head.startsWith("ref: refs/heads/")) {
            return head.substring("ref: refs/heads/".length());
        }
        // Detached HEAD: the file contains the commit SHA.
        return head;
    }

    /**
     * Gets the commit SHA pointed to by {@code HEAD}.
     *
     * <p>Handles loose refs under {@code <gitDir>/refs/...}, packed refs in {@code <gitDir>/packed-refs},
     * symbolic indirection (a ref file that itself contains {@code ref: ...}), and detached HEAD.</p>
     *
     * @param repositoryPath A path inside the Git repository.
     * @return The hex-encoded commit SHA.
     * @throws IOException If the {@code .git} directory cannot be found, the ref cannot be resolved,
     *                     or the symbolic chain is deeper than {@value #MAX_REF_DEPTH}.
     */
    public static String getHeadCommit(final Path repositoryPath) throws IOException {
        final Path gitDir = findGitDir(repositoryPath);
        String value = readHead(gitDir);
        for (int i = 0; i < MAX_REF_DEPTH; i++) {
            if (!value.startsWith(REF_PREFIX)) {
                return value;
            }
            value = resolveRef(gitDir, value.substring(REF_PREFIX.length()));
        }
        throw new IOException("Symbolic ref chain exceeds " + MAX_REF_DEPTH + " hops in: " + gitDir);
    }

    /**
     * Returns the Git tree hash for the given directory.
     *
     * @param path A directory path.
     * @return A hex-encoded SHA-1 tree hash.
     * @throws IOException If the path is not a directory or an I/O error occurs.
     */
    public static String gitTree(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IOException("Path is not a directory: " + path);
        }
        final MessageDigest digest = DigestUtils.getSha1Digest();
        return Hex.encodeHexString(GitIdentifiers.treeId(digest, path));
    }

    /**
     * Reads and trims the {@code HEAD} file of the given Git directory.
     *
     * @param gitDir The {@code .git} directory.
     * @return The trimmed contents of {@code <gitDir>/HEAD}.
     * @throws IOException If the file cannot be read.
     */
    private static String readHead(final Path gitDir) throws IOException {
        return new String(Files.readAllBytes(gitDir.resolve("HEAD")), StandardCharsets.UTF_8).trim();
    }

    /**
     * Returns the directory that holds shared repository state (loose refs, {@code packed-refs}).
     * In a linked worktree this is read from {@code <gitDir>/commondir}; otherwise it is
     * {@code gitDir} itself.
     *
     * @param gitDir The {@code .git} directory.
     * @return The shared-state directory.
     * @throws IOException If {@code commondir} exists but cannot be read.
     */
    private static Path resolveCommonDir(final Path gitDir) throws IOException {
        final Path commonDir = gitDir.resolve("commondir");
        if (Files.isRegularFile(commonDir)) {
            final String value = new String(Files.readAllBytes(commonDir), StandardCharsets.UTF_8).trim();
            return gitDir.resolve(value).normalize();
        }
        return gitDir;
    }

    /**
     * Resolves a single ref (e.g. {@code refs/heads/foo}) to its stored value.
     *
     * <p>The return value is either a commit SHA or another {@code ref: ...} line, which the caller continues to resolve.</p>
     *
     * <p>In a linked worktree, loose and packed refs are stored in the "common dir" (usually the
     * main repository's {@code .git}), which is pointed to by {@code <gitDir>/commondir}.</p>
     *
     * @param gitDir  The {@code .git} directory.
     * @param refPath The ref path relative to the common dir (e.g. {@code refs/heads/main}).
     * @return Either a commit SHA or another {@code ref: ...} line to be resolved by the caller.
     * @throws IOException If the ref is not found as a loose file or in {@code packed-refs}.
     */
    private static String resolveRef(final Path gitDir, final String refPath) throws IOException {
        final Path refsDir = resolveCommonDir(gitDir);
        final Path refFile = refsDir.resolve(refPath);
        if (Files.isRegularFile(refFile)) {
            return new String(Files.readAllBytes(refFile), StandardCharsets.UTF_8).trim();
        }
        final Path packed = refsDir.resolve("packed-refs");
        if (Files.isRegularFile(packed)) {
            try (BufferedReader reader = Files.newBufferedReader(packed, StandardCharsets.UTF_8)) {
                // packed-refs format: one ref per line as "<sha> <refname>", with '#' header lines,
                // blank lines, and "^<sha>" peeled-tag continuation lines that we skip.
                // See https://git-scm.com/docs/gitrepository-layout
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '^') {
                        continue;
                    }
                    final int space = line.indexOf(' ');
                    if (space > 0 && refPath.equals(line.substring(space + 1))) {
                        return line.substring(0, space);
                    }
                }
            }
        }
        throw new IOException("Cannot resolve ref: " + refPath);
    }

    /**
     * Converts an SCM URI to a download URI suffixed with the current branch name.
     *
     * @param scmUri         A Maven SCM URI starting with {@code scm:git}.
     * @param repositoryPath A path inside the Git repository.
     * @return A download URI of the form {@code git+<url>@<branch>}.
     * @throws IOException If the current branch cannot be determined.
     */
    public static String scmToDownloadUri(final String scmUri, final Path repositoryPath) throws IOException {
        if (!scmUri.startsWith(SCM_GIT_PREFIX)) {
            throw new IllegalArgumentException("Invalid scmUri: " + scmUri);
        }
        final String currentBranch = getCurrentBranch(repositoryPath);
        return "git+" + scmUri.substring(SCM_GIT_PREFIX.length()) + "@" + currentBranch;
    }

    /**
     * No instances.
     */
    private GitUtils() {
        // no instantiation
    }
}
