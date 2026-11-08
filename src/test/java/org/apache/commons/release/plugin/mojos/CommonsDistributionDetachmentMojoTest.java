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
package org.apache.commons.release.plugin.mojos;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Properties;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link CommonsDistributionDetachmentMojo}.
 */
@MojoTest
public class CommonsDistributionDetachmentMojoTest {

    /**
     * Replaces the vote target's external SVN and Git commands.
     */
    public static class StubExecTask extends ExecTask {

        @Override
        public void execute() {
            // Repository metadata is unrelated to checksum rendering.
        }
    }

    private static final String COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH = "target/testing-commons-release-plugin";

    @BeforeEach
    public void setUp() throws Exception {
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    @InjectMojo(goal = "detach-distributions", pom = "src/test/resources/mojos/detach-distributions/detach-distributions-disabled.xml")
    public void testDisabled(final CommonsDistributionDetachmentMojo mojo) throws Exception {
        mojo.execute();
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        assertFalse(testingDirectory.exists());
    }

    @Test
    @InjectMojo(goal = "detach-distributions", pom = "src/test/resources/mojos/detach-distributions/detach-distributions.xml")
    public void testSha512PropertiesInVoteTxt(final CommonsDistributionDetachmentMojo mojo, @TempDir final Path directory) throws Exception {
        mojo.execute();
        final Path sha512File = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH, "sha512.properties").toPath();
        final Properties actual = new Properties();
        try (InputStream input = Files.newInputStream(sha512File)) {
            actual.load(input);
        }
        final Properties expected = new Properties();
        final String[] suffixes = {"-bin.tar.gz", "-bin.zip", "-src.tar.gz", "-src.zip", ".jar", ".pom", "-javadoc.jar", "-sources.jar"};
        for (final String suffix : suffixes) {
            final String name = "commons-text-1.4" + suffix;
            final Path artifact = new File("src/test/resources/mojos/detach-distributions/target", name).toPath();
            expected.setProperty(name, Hex.encodeHexString(MessageDigest.getInstance("SHA-512").digest(Files.readAllBytes(artifact))));
        }
        // Exact equality also excludes signatures and unexpected artifacts.
        assertEquals(expected, actual);

        final Path voteChecksums = directory.resolve("target/commons-release-plugin/sha512.properties");
        Files.createDirectories(voteChecksums.getParent());
        Files.copy(sha512File, voteChecksums);
        final Project project = new Project();
        project.init();
        ProjectHelper.configureProject(project, new File("src/main/scripts/generate-xdocs.build.xml"));
        project.setBaseDir(directory.toFile());
        project.addTaskDefinition("exec", StubExecTask.class);
        project.setProperty("commons.nexus.repo.id", "1234");
        project.setProperty("project.groupId", "org.apache.commons");
        project.setProperty("project.artifactId", "commons-text");
        project.setProperty("svn.rc.revision", "12345");
        project.setProperty("git.tag.commit", "test-commit");
        project.executeTarget("vote-txt");

        final String vote = new String(Files.readAllBytes(directory.resolve("target/VOTE.txt")), StandardCharsets.UTF_8);
        final String checksums = new String(Files.readAllBytes(sha512File), StandardCharsets.ISO_8859_1);
        assertTrue(vote.contains(checksums));
        assertFalse(vote.contains("@SHA512LIST@"));
        assertFalse(vote.contains("${commons.sha512list}"));
    }

    @Test
    @InjectMojo(goal = "detach-distributions", pom = "src/test/resources/mojos/detach-distributions/detach-distributions.xml")
    public void testSuccess(final CommonsDistributionDetachmentMojo mojo) throws Exception {
        mojo.execute();
        final File detachedSrcTarGz = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz");
        final File detachedSrcTarGzAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz.asc");
        final File detachedSrcTarGzSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz.sha512");
        final File detachedSrcZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip");
        final File detachedSrcZipAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip.asc");
        final File detachedSrcZipSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip.sha512");
        final File detachedBinTarGz = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz");
        final File detachedBinTarGzAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz.asc");
        final File detachedBinTarGzSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz.sha512");
        final File detachedBinZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip");
        final File detachedBinZipAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip.asc");
        final File detachedBinZipSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip.sha512");
        final File notDetachedMockAttachedFile = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4.jar");
        final File sha512Properties = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/sha512.properties");
        assertTrue(detachedSrcTarGz.exists());
        assertTrue(detachedSrcTarGzAsc.exists());
        assertTrue(detachedSrcTarGzSha512.exists());
        assertTrue(detachedSrcZip.exists());
        assertTrue(detachedSrcZipAsc.exists());
        assertTrue(detachedSrcZipSha512.exists());
        assertTrue(detachedBinTarGz.exists());
        assertTrue(detachedBinTarGzAsc.exists());
        assertTrue(detachedBinTarGzSha512.exists());
        assertTrue(detachedBinZip.exists());
        assertTrue(detachedBinZipAsc.exists());
        assertTrue(detachedBinZipSha512.exists());
        assertTrue(sha512Properties.exists());
        assertFalse(notDetachedMockAttachedFile.exists());
    }
}
