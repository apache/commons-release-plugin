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
            // noop
        }

        @Override
        protected void after() {
            // noop
        }
    };

    private CommonsDistributionDetachmentMojo detachmentMojo;

    private CommonsDistributionStagingMojo mojoForTest;

    @Before
    public void setUp() throws Exception {
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        if (testingDirectory.exists()) {
            FileUtils.deleteDirectory(testingDirectory);
        }
    }

    @Test
    public void testSuccess() throws Exception {
        final File testPom = new File("src/test/resources/mojos/stage-distributions/stage-distributions.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        final File detachmentPom = new File("src/test/resources/mojos/detach-distributions/detach-distributions.xml");
        assertNotNull(detachmentPom);
        assertTrue(detachmentPom.exists());
        mojoForTest = (CommonsDistributionStagingMojo) rule.lookupMojo("stage-distributions", testPom);
        detachmentMojo = (CommonsDistributionDetachmentMojo) rule.lookupMojo("detach-distributions", detachmentPom);
        detachmentMojo.execute();
        final File releaseNotesBasedir = new File("src/test/resources/mojos/stage-distributions/");
        mojoForTest.setBaseDir(releaseNotesBasedir);
        mojoForTest.execute();
        assertRequisiteFilesExist();
    }

    @Test
    public void testDisabled() throws Exception {
        final File testPom = new File("src/test/resources/mojos/stage-distributions/stage-distributions-disabled.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojoForTest = (CommonsDistributionStagingMojo) rule.lookupMojo("stage-distributions", testPom);
        mojoForTest.execute();
        final File testingDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH);
        assertFalse(testingDirectory.exists());
    }

    private void assertRequisiteFilesExist() {
        final File targetScmDirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1");
        final File releaseNotes = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/RELEASE-NOTES.txt");
        final File readmeHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/README.html");
        final File headerHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/HEADER.html");
        final File signatureValidatorScript = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/signature-validator.sh");
        final File binariesReadmeHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/README.html");
        final File binariesHeaderHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/HEADER.html");
        final File binTar = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/commons-text-1.4-bin.tar.gz");
        final File binTarASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/commons-text-1.4-bin.tar.gz.asc");
        final File binTarSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/commons-text-1.4-bin.tar.gz.sha512");
        final File binZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/commons-text-1.4-bin.zip");
        final File binZipASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/commons-text-1.4-bin.zip.asc");
        final File binZipSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/commons-text-1.4-bin.zip.sha512");
        final File sourcesReadmeHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/README.html");
        final File sourceHeaderHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/binaries/HEADER.html");
        final File srcTar = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/source/commons-text-1.4-src.tar.gz");
        final File srcTarASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/source/commons-text-1.4-src.tar.gz.asc");
        final File srcTarSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/source/commons-text-1.4-src.tar.gz.sha512");
        final File srcZip = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/source/commons-text-1.4-src.zip");
        final File srcZipASC = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/source/commons-text-1.4-src.zip.asc");
        final File srcZipSha512 = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/source/commons-text-1.4-src.zip.sha512");
        final File site = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/site");
        final File siteIndexHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/site/index.html");
        final File siteSubdirectory = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/site/subdirectory");
        final File siteSubdirectoryIndexHtml = new File(COMMONS_RELEASE_PLUGIN_TEST_DIR_PATH + "/scm/1.0-SNAPSHOT-RC1/site/subdirectory/index.html");
        assertTrue(targetScmDirectory.exists());
        assertTrue(releaseNotes.exists());
        assertTrue(readmeHtml.exists());
        assertTrue(headerHtml.exists());
        assertTrue(signatureValidatorScript.exists());
        assertTrue(binariesReadmeHtml.exists());
        assertTrue(binariesHeaderHtml.exists());
        assertTrue(binTar.exists());
        assertTrue(binTarASC.exists());
        assertTrue(binTarSha512.exists());
        assertTrue(binZip.exists());
        assertTrue(binZipASC.exists());
        assertTrue(binZipSha512.exists());
        assertTrue(sourcesReadmeHtml.exists());
        assertTrue(sourceHeaderHtml.exists());
        assertTrue(srcTar.exists());
        assertTrue(srcTarASC.exists());
        assertTrue(srcTarSha512.exists());
        assertTrue(srcZip.exists());
        assertTrue(srcZipASC.exists());
        assertTrue(srcZipSha512.exists());
        assertTrue(site.exists());
        assertTrue(siteIndexHtml.exists());
        assertTrue(siteSubdirectory.exists());
        assertTrue(siteSubdirectoryIndexHtml.exists());
    }
}
