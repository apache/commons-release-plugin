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
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;

/**
 * Stub for {@link MavenProject} for the {@link CommonsDistributionDetachmentMojoTest}. See the
 * testing pom,
 * <code>src/test/resources/detach-distributions/detach-distributions.xml</code> for the declared
 * usage of
 * this class.
 *
 * @since 1.0
 */
public class DistributionDetachmentProjectStub extends MavenProjectStub {

    public static class DistributionDetachmentArtifactStub extends ArtifactStub {

        private final File artifact;

        private final String version;

        private final String classifier;

        private final String type;

        public DistributionDetachmentArtifactStub(final File file, final String type, final String groupId,
                final String artifactId, final String classifier, final String version) {
            this.setArtifactId(artifactId);
            this.setGroupId(groupId);
            this.setArtifactHandler(new DefaultArtifactHandlerStub(type, classifier));
            this.artifact = file;
            this.type = type;
            this.classifier = classifier;
            this.version = version;
        }

        @Override
        public String getClassifier() {
            return this.classifier;
        }

        @Override
        public File getFile() {
            return this.artifact;
        }

        @Override
        public String getType() {
            return this.type;
        }

        @Override
        public String getVersion() {
            return this.version;
        }
    }

    private List<Artifact> attachedArtifacts;

    @Override
    public String getArtifactId() {
        return "commons-text";
    }

    @Override
    public List<Artifact> getAttachedArtifacts() {
        attachedArtifacts = new ArrayList<>();
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-bin.tar.gz"),
                "tar.gz",
                "org.apache.commons",
                "commons-text",
                "bin", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-bin.tar.gz.asc"),
                "tar.gz.asc",
                "org.apache.commons",
                "commons-text",
                "bin", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-bin.zip"),
                "zip",
                "org.apache.commons",
                "commons-text",
                "bin", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-bin.zip.asc"),
                "zip.asc",
                "org.apache.commons",
                "commons-text",
                "bin", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-src.tar.gz"),
                "tar.gz",
                "org.apache.commons",
                "commons-text",
                "src", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-src.tar.gz.asc"),
                "tar.gz.asc",
                "org.apache.commons",
                "commons-text",
                "src", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-src.zip"),
                "zip",
                "org.apache.commons",
                "commons-text",
                "src", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-src.zip.asc"),
                "zip.asc",
                "org.apache.commons",
                "commons-text",
                "src", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4.jar"),
                "jar",
                "org.apache.commons",
                "commons-text",
                "jar", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4.jar.asc"),
                "jar.asc",
                "org.apache.commons",
                "commons-text",
                "jar", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4.pom"),
                "pom",
                "org.apache.commons",
                "commons-text",
                "pom", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4.pom.asc"),
                "pom.asc",
                "org.apache.commons",
                "commons-text",
                "pom", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-javadoc.jar"),
                "jar",
                "org.apache.commons",
                "commons-text",
                "javadoc", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-javadoc.jar.asc"),
                "jar.asc",
                "org.apache.commons",
                "commons-text",
                "javadoc", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-sources.jar"),
                "jar",
                "org.apache.commons",
                "commons-text",
                "sources", "1.4"
            )
        );
        attachedArtifacts.add(
            new DistributionDetachmentArtifactStub(
                new File("src/test/resources/mojos/detach-distributions/target/commons-text-1.4-sources.jar.asc"),
                "jar.asc",
                "org.apache.commons",
                "commons-text",
                "sources", "1.4"
            )
        );
        return attachedArtifacts;
    }

    @Override
    public String getGroupId() {
        return "org.apache.commons";
    }

    @Override
    public String getUrl() {
        return "https://commons.apache.org/proper/commons-text/";
    }

    @Override
    public String getVersion() {
        return "1.4";
    }
}
