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
package org.apache.commons.release.plugin.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.release.plugin.mojos.CommonsDistributionDetachmentMojoTest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * Stub for {@link MavenProject} for the {@link CommonsDistributionDetachmentMojoTest}. See the
 * testing pom,
 * <code>src/test/resources/detach-distributions/detach-distributions.xml</code> for the declared
 * usage of
 * this class.
 *
 * @author chtompki
 * @since 1.0
 */
public class DistributionDetachmentProjectStub extends MavenProjectStub {

    private List<Artifact> attachedArtifacts;

    @Override
    public String getArtifactId() {
        return "commons-text";
    }

    @Override
    public String getVersion() {
        return "1.4";
    }

    @Override
    public String getUrl() {
        return "http://commons.apache.org/proper/commons-text/";
    }

    @Override
    public List<Artifact> getAttachedArtifacts() {
        attachedArtifacts = new ArrayList<>();
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedFile.html"),
                "html",
                "mockAttachedFile"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedTar-src.tar.gz"),
                "tar.gz",
                "mockAttachedTar"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedTar-src.tar.gz.asc"),
                "tar.gz.asc",
                "mockAttachedTar"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedTar-bin.tar.gz"),
                "tar.gz",
                "mockAttachedTar"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedTar-bin.tar.gz.asc"),
                "tar.gz.asc",
                "mockAttachedTar"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedZip-src.zip"),
                "zip",
                "mockAttachedZip"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedZip-src.zip.asc"),
                "zip.asc",
                "mockAttachedZip"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedZip-bin.zip"),
                "zip",
                "mockAttachedZip"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File(
                "src/test/resources/mojos/detach-distributions/target/mockAttachedZip-bin.zip.asc"),
                "zip.asc",
                "mockAttachedZip"
            )
        );
        return attachedArtifacts;
    }

    public class DistributionDetachmentArtifactStub extends ArtifactStub {

        private final File artifact;

        private final String type;

        public DistributionDetachmentArtifactStub(File file, String type, String artifactId) {
            this.setArtifactId(artifactId);
            this.artifact = file;
            this.type = type;
        }

        @Override
        public File getFile() {
            return this.artifact;
        }

        @Override
        public String getType() {
            return this.type;
        }
    }
}
