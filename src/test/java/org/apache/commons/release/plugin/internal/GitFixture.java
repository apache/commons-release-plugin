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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.output.NullOutputStream;

/**
 * Builds real Git fixtures on disk by invoking the {@code git} CLI via Commons Exec
 */
final class GitFixture {

    static final String REPO_BRANCH = "foo";
    static final String WORKTREE_BRANCH = "bar";
    static final String SUBDIR = "subdir";
    /**
     * SHA-1 of the single commit produced by {@link #createRepoAndWorktree}; deterministic thanks to {@link #ENV}.
     */
    static final String INITIAL_COMMIT_SHA = "a2782b3461d2ed2a81193da1139f65bf9d2befc2";

    /**
     * Process environment with fixed author/committer dates so commit SHAs are stable across runs.
     */
    private static final Map<String, String> ENV;

    static {
        try {
            final Map<String, String> env = EnvironmentUtils.getProcEnvironment();
            env.put("GIT_AUTHOR_DATE", "2026-01-01T00:00:00Z");
            env.put("GIT_COMMITTER_DATE", "2026-01-01T00:00:00Z");
            ENV = env;
        } catch (final IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Creates a Git repo for testing.
     *
     * @param repo     Path to the repository to create
     * @param worktree Path to a separate worktree to create
     */
    static void createRepoAndWorktree(final Path repo, final Path worktree) throws IOException {
        final Path subdir = repo.resolve(SUBDIR);
        Files.createDirectories(subdir);
        git(repo, "init", "-q", ".");
        // Put HEAD on 'foo' before the first commit (portable to older git without --initial-branch).
        git(repo, "symbolic-ref", "HEAD", "refs/heads/" + REPO_BRANCH);
        git(repo, "config", "user.email", "test@example.invalid");
        git(repo, "config", "user.name", "Test");
        git(repo, "config", "commit.gpgsign", "false");
        final Path readme = subdir.resolve("README");
        Files.write(readme, "hi\n".getBytes(StandardCharsets.UTF_8));
        git(repo, "add", repo.relativize(readme).toString());
        git(repo, "commit", "-q", "-m", "init");
        git(repo, "branch", WORKTREE_BRANCH);
        git(repo, "worktree", "add", "-q", repo.relativize(worktree).toString(), "bar");
    }

    /**
     * Runs {@code git} with the given args; stdout is discarded, stderr is forwarded to {@link System#err}.
     */
    static void git(final Path workingDir, final String... args) throws IOException {
        final CommandLine cmd = new CommandLine("git");
        for (final String a : args) {
            cmd.addArgument(a, false);
        }
        final Executor exec = DefaultExecutor.builder().setWorkingDirectory(workingDir.toFile()).get();
        exec.setStreamHandler(new PumpStreamHandler(NullOutputStream.INSTANCE, System.err));
        exec.execute(cmd, ENV);
    }

    private GitFixture() {
    }
}
