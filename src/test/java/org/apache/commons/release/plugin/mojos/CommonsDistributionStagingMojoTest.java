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
 * Unit tests for {@link CommonsDistributionStagingMojo}.
 *
 * @author chtompki
 * @since 1.0.
 */
public class CommonsDistributionStagingMojoTest {

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

    private CommonsDistributionDetachmentMojo detachmentMojo;

    private CommonsDistributionStagingMojo mojoForTest;

    @Before
    public void setUp() throws Exception {
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    public void testSuccess() throws Exception {
        File testPom = new File("src/test/resources/mojos/stage-distributions/stage-distributions.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        File detachmentPom = new File("src/test/resources/mojos/detach-distributions/detach-distributions.xml");
        assertNotNull(detachmentPom);
        assertTrue(detachmentPom.exists());
        mojoForTest = (CommonsDistributionStagingMojo) rule.lookupMojo("stage-distributions", testPom);
        detachmentMojo = (CommonsDistributionDetachmentMojo) rule.lookupMojo("detach-distributions", detachmentPom);
        detachmentMojo.execute();
        File releaseNotesBasedir = new File("src/test/resources/mojos/stage-distributions/");
        mojoForTest.setBaseDir(releaseNotesBasedir);
        mojoForTest.execute();
        File targetScmDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm");
        File releaseNotes = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/RELEASE-NOTES.txt");
        File readmeHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/README.html");
        File headerHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/HEADER.html");
        File binariesReadmeHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/README.html");
        File binariesHeaderHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/HEADER.html");
        File binTar = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedTar-bin.tar.gz");
        File binTarASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedTar-bin.tar.gz.asc");
        File binTarMD5 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedTar-bin.tar.gz.md5");
        File binTarSHA1 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedTar-bin.tar.gz.sha1");
        File binTarSHA256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedTar-bin.tar.gz.sha256");
        File binZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedZip-bin.zip");
        File binZipASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedZip-bin.zip.asc");
        File binZipMD5 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedZip-bin.zip.md5");
        File binZipSHA1 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedZip-bin.zip.sha1");
        File binZipSHA256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/mockAttachedZip-bin.zip.sha256");
        File sourcesReadmeHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/README.html");
        File sourceHeaderHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/binaries/HEADER.html");
        File srcTar = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedTar-src.tar.gz");
        File srcTarASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedTar-src.tar.gz.asc");
        File srcTarMD5 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedTar-src.tar.gz.md5");
        File srcTarSHA1 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedTar-src.tar.gz.sha1");
        File srcTarSHA256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedTar-src.tar.gz.sha256");
        File srcZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedZip-src.zip");
        File srcZipASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedZip-src.zip.asc");
        File srcZipMD5 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedZip-src.zip.md5");
        File srcZipSHA1 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedZip-src.zip.sha1");
        File srcZipSHA256 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/source/mockAttachedZip-src.zip.sha256");
        File site = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/site");
        File siteIndexHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/site/index.html");
        File siteSubdirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/site/subdirectory");
        File siteSubdirectoryIndexHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/site/subdirectory/index.html");
        assertTrue(targetScmDirectory.exists());
        assertTrue(releaseNotes.exists());
        assertTrue(readmeHtml.exists());
        assertTrue(headerHtml.exists());
        assertTrue(binariesReadmeHtml.exists());
        assertTrue(binariesHeaderHtml.exists());
        assertTrue(binTar.exists());
        assertTrue(binTarASC.exists());
        assertTrue(binTarMD5.exists());
        assertTrue(binTarSHA1.exists());
        assertTrue(binTarSHA256.exists());
        assertTrue(binZip.exists());
        assertTrue(binZipASC.exists());
        assertTrue(binZipMD5.exists());
        assertTrue(binZipSHA1.exists());
        assertTrue(binZipSHA256.exists());
        assertTrue(sourcesReadmeHtml.exists());
        assertTrue(sourceHeaderHtml.exists());
        assertTrue(srcTar.exists());
        assertTrue(srcTarASC.exists());
        assertTrue(srcTarMD5.exists());
        assertTrue(srcTarSHA1.exists());
        assertTrue(srcTarSHA256.exists());
        assertTrue(srcZip.exists());
        assertTrue(srcZipASC.exists());
        assertTrue(srcZipMD5.exists());
        assertTrue(srcZipSHA1.exists());
        assertTrue(srcZipSHA256.exists());
        assertTrue(site.exists());
        assertTrue(siteIndexHtml.exists());
        assertTrue(siteSubdirectory.exists());
        assertTrue(siteSubdirectoryIndexHtml.exists());
    }

    @Test
    public void testDisabled() throws Exception {
        File testPom = new File("src/test/resources/mojos/stage-distributions/stage-distributions-disabled.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojoForTest = (CommonsDistributionStagingMojo) rule.lookupMojo("stage-distributions", testPom);
        mojoForTest.execute();
        File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        assertFalse(testingDirectory.exists());
    }
}
