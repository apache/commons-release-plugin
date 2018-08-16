/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.release.plugin.mojos;

import org.apache.maven.plugin.testing.MojoRule;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CommonsDistributionDetachmentMojo}.
 *
 * @author chtompki
 */
public class CommonsDistributionDetachmentMojoTest {

    private static final String COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH = "target/testing-commons-release-plugin";

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    private CommonsDistributionDetachmentMojo mojo;

    @Before
    public void setUp() throws Exception {
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    public void testSuccess() throws Exception {
        File testPom = new File("src/test/resources/mojos/detach-distributions/detach-distributions.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsDistributionDetachmentMojo) rule.lookupMojo("detach-distributions", testPom);
        mojo.execute();
        File detachedSrcTarGz = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz");
        File detachedSrcTarGzAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz.asc");
        File detachedSrcTarGzSha256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz.sha256");
        File detachedSrcTarGzSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.tar.gz.sha512");
        File detachedSrcZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip");
        File detachedSrcZipAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip.asc");
        File detachedSrcZipSha256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip.sha256");
        File detachedSrcZipSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-src.zip.sha512");
        File detachedBinTarGz = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz");
        File detachedBinTarGzAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz.asc");
        File detachedBinTarGzSha256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz.sha256");
        File detachedBinTarGzSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.tar.gz.sha512");
        File detachedBinZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip");
        File detachedBinZipAsc = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip.asc");
        File detachedBinZipSha256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip.sha256");
        File detachedBinZipSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4-bin.zip.sha512");
        File notDetachedMockAttachedFile = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/commons-text-1.4.jar");
        File sha256Properties = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/sha256.properties");
        File sha512Properties = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/sha512.properties");
        assertTrue(detachedSrcTarGz.exists());
        assertTrue(detachedSrcTarGzAsc.exists());
        assertTrue(detachedSrcTarGzSha256.exists());
        assertTrue(detachedSrcTarGzSha512.exists());
        assertTrue(detachedSrcZip.exists());
        assertTrue(detachedSrcZipAsc.exists());
        assertTrue(detachedSrcZipSha256.exists());
        assertTrue(detachedSrcZipSha512.exists());
        assertTrue(detachedBinTarGz.exists());
        assertTrue(detachedBinTarGzAsc.exists());
        assertTrue(detachedBinTarGzSha256.exists());
        assertTrue(detachedBinTarGzSha512.exists());
        assertTrue(detachedBinZip.exists());
        assertTrue(detachedBinZipAsc.exists());
        assertTrue(detachedBinZipSha256.exists());
        assertTrue(detachedBinZipSha512.exists());
        assertTrue(sha256Properties.exists());
        assertTrue(sha512Properties.exists());
        assertFalse(notDetachedMockAttachedFile.exists());
    }

    @Test
    public void testDisabled() throws Exception {
        File testPom = new File("src/test/resources/mojos/detach-distributions/detach-distributions-disabled.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsDistributionDetachmentMojo) rule.lookupMojo("detach-distributions", testPom);
        mojo.execute();
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        assertFalse(testingDirectory.exists());
    }
}
