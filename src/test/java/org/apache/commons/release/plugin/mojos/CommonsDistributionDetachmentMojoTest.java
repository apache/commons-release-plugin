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

import java.io.File;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CommonsDistributionDetachmentMojo}.
 */
@MojoTest
public class CommonsDistributionDetachmentMojoTest {

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
