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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class GitUtilsTest {

    private static Path repo;
    @TempDir
    static Path tempDir;
    private static Path worktree;

    @BeforeAll
    static void setUp() throws IOException {
        repo = tempDir.resolve("repo");
        worktree = tempDir.resolve("worktree");
        GitFixture.createRepoAndWorktree(repo, worktree);
    }

    static Stream<Arguments> testGetCurrentBranch() {
        return Stream.of(Arguments.of(repo, GitFixture.REPO_BRANCH), Arguments.of(repo.resolve(GitFixture.SUBDIR), GitFixture.REPO_BRANCH),
                Arguments.of(worktree, GitFixture.WORKTREE_BRANCH), Arguments.of(worktree.resolve(GitFixture.SUBDIR), GitFixture.WORKTREE_BRANCH));
    }

    static Stream<Arguments> testGetHeadCommit() {
        return Stream.of(
                Arguments.of(repo, GitFixture.INITIAL_COMMIT_SHA),
                Arguments.of(repo.resolve(GitFixture.SUBDIR), GitFixture.INITIAL_COMMIT_SHA),
                Arguments.of(worktree, GitFixture.INITIAL_COMMIT_SHA),
                Arguments.of(worktree.resolve(GitFixture.SUBDIR), GitFixture.INITIAL_COMMIT_SHA));
    }

    static Stream<Arguments> testScmToDownloadUri() {
        return Stream.of(
                Arguments.of("scm:git:https://gitbox.apache.org/repos/asf/commons-release-plugin.git",
                        repo,
                        "git+https://gitbox.apache.org/repos/asf/commons-release-plugin.git@" + GitFixture.REPO_BRANCH),
                Arguments.of("scm:git:git@github.com:apache/commons-release-plugin.git",
                        repo,
                        "git+git@github.com:apache/commons-release-plugin.git@" + GitFixture.REPO_BRANCH),
                Arguments.of("scm:git:ssh://git@github.com/apache/commons-release-plugin.git",
                        worktree,
                        "git+ssh://git@github.com/apache/commons-release-plugin.git@" + GitFixture.WORKTREE_BRANCH));
    }

    @ParameterizedTest
    @MethodSource
    void testGetCurrentBranch(final Path repo, final String expectedBranchName) throws Exception {
        assertEquals(expectedBranchName, GitUtils.getCurrentBranch(repo));
    }

    @Test
    void testGetCurrentBranchDetachedHead() throws IOException {
        // Build a fresh repo so we don't mutate HEAD shared with the parameterized tests.
        final Path detachedRepo = tempDir.resolve("detached-repo");
        final Path detachedWorktree = tempDir.resolve("detached-worktree");
        GitFixture.createRepoAndWorktree(detachedRepo, detachedWorktree);
        GitFixture.git(detachedRepo, "checkout", "-q", "--detach", "HEAD");
        assertEquals(GitFixture.INITIAL_COMMIT_SHA, GitUtils.getCurrentBranch(detachedRepo));
    }

    @ParameterizedTest
    @MethodSource
    void testGetHeadCommit(final Path repositoryPath, final String expectedSha) throws IOException {
        assertEquals(expectedSha, GitUtils.getHeadCommit(repositoryPath));
    }

    @Test
    void testGetHeadCommitDetachedHead() throws IOException {
        final Path detachedRepo = tempDir.resolve("detached-head-commit-repo");
        final Path detachedWorktree = tempDir.resolve("detached-head-commit-worktree");
        GitFixture.createRepoAndWorktree(detachedRepo, detachedWorktree);
        GitFixture.git(detachedRepo, "checkout", "-q", "--detach", "HEAD");
        assertEquals(GitFixture.INITIAL_COMMIT_SHA, GitUtils.getHeadCommit(detachedRepo));
    }

    @Test
    void testGetHeadCommitPackedRefs() throws IOException {
        final Path packedRepo = tempDir.resolve("packed-repo");
        final Path packedWorktree = tempDir.resolve("packed-worktree");
        GitFixture.createRepoAndWorktree(packedRepo, packedWorktree);
        // Move all loose refs (branches, tags) into .git/packed-refs and delete the loose files.
        GitFixture.git(packedRepo, "pack-refs", "--all", "--prune");
        assertEquals(GitFixture.INITIAL_COMMIT_SHA, GitUtils.getHeadCommit(packedRepo));
    }

    @ParameterizedTest
    @MethodSource
    void testScmToDownloadUri(final String scmUri, final Path repositoryPath, final String expectedDownloadUri) throws IOException {
        assertEquals(expectedDownloadUri, GitUtils.scmToDownloadUri(scmUri, repositoryPath));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "scm:svn:https://svn.apache.org/repos/asf/commons-release-plugin",
            "scm:hg:https://example.com/repo",
            "https://github.com/apache/commons-release-plugin.git",
            "git:https://github.com/apache/commons-release-plugin.git",
            ""
    })
    void testScmToDownloadUriRejectsNonGit(final String scmUri) {
        assertThrows(IllegalArgumentException.class, () -> GitUtils.scmToDownloadUri(scmUri, repo));
    }

    @Test
    void throwsWhenNoGitDirectoryFound() throws IOException {
        final Path plain = Files.createDirectories(tempDir.resolve("plain"));
        assertThrows(IOException.class, () -> GitUtils.getCurrentBranch(plain));
    }
}
