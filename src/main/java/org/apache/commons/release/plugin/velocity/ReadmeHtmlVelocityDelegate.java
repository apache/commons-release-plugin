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
package org.apache.commons.release.plugin.velocity;

import java.io.Writer;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * This class' purpose is to generate the <code>README.html</code> that moves along with the
 * release for the sake of downloading the release from the distribution area.
 *
 * @author chtompki
 * @since 1.3
 */
public class ReadmeHtmlVelocityDelegate {
    /** The location of the velocity template for this class. */
    private static final String TEMPLATE = "resources/org/apache/commons/release/plugin"
                                         + "/velocity/README.vm";
    /** This is supposed to represent the maven artifactId. */
    private final String artifactId;
    /** This is supposed to represent the maven version of the release. */
    private final String version;
    /** The url of the site that gets set into the <code>README.html</code>. */
    private final String siteUrl;

    /**
     * The private constructor to be used by the {@link ReadmeHtmlVelocityDelegateBuilder}.
     *
     * @param artifactId sets the {@link ReadmeHtmlVelocityDelegate#artifactId}.
     * @param version sets the {@link ReadmeHtmlVelocityDelegate#version}.
     * @param siteUrl sets the {@link ReadmeHtmlVelocityDelegate#siteUrl}.
     */
    private ReadmeHtmlVelocityDelegate(String artifactId, String version, String siteUrl) {
        this.artifactId = artifactId;
        this.version = version;
        this.siteUrl = siteUrl;
    }

    /**
     * Gets the {@link ReadmeHtmlVelocityDelegateBuilder} for constructing the {@link ReadmeHtmlVelocityDelegate}.
     *
     * @return the {@link ReadmeHtmlVelocityDelegateBuilder}.
     */
    public static ReadmeHtmlVelocityDelegateBuilder builder() {
        return new ReadmeHtmlVelocityDelegateBuilder();
    }

    /**
     * Renders the <code>README.vm</code> velocity template with the variables constructed with the
     * {@link ReadmeHtmlVelocityDelegateBuilder}.
     *
     * @param writer is the {@link Writer} to which we wish to render the <code>README.vm</code> template.
     * @return a reference to the {@link Writer} passed in.
     */
    public Writer render(Writer writer) {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        Template template = ve.getTemplate(TEMPLATE);
        String[] splitArtifactId = artifactId.split("-");
        String wordCommons = splitArtifactId[0];
        String artifactShortName = splitArtifactId[1];
        String artifactIdWithFirstLetterscapitalized =
                StringUtils.capitalize(wordCommons)
                        + "-"
                        + artifactShortName.toUpperCase();
        VelocityContext context = new VelocityContext();
        context.internalPut("artifactIdWithFirstLetterscapitalized", artifactIdWithFirstLetterscapitalized);
        context.internalPut("artifactShortName", artifactShortName.toUpperCase());
        context.internalPut("artifactId", artifactId);
        context.internalPut("version", version);
        context.internalPut("siteUrl", siteUrl);
        template.merge(context, writer);
        return writer;
    }

    /**
     * A builder class for instantiation of the {@link ReadmeHtmlVelocityDelegate}.
     */
    public static class ReadmeHtmlVelocityDelegateBuilder {
        /** The maven artifactId to use in the <code>README.vm</code> template. */
        private String artifactId;
        /** The maven version to use in the <code>README.vm</code> template. */
        private String version;
        /** The site url to use in the <code>README.vm</code> template. */
        private String siteUrl;

        /**
         * Private constructor for using the builder through the {@link ReadmeHtmlVelocityDelegate#builder()}
         * method.
         */
        private ReadmeHtmlVelocityDelegateBuilder() {
            super();
        }

        /**
         * Adds the artifactId to the {@link ReadmeHtmlVelocityDelegate}.
         * @param artifactId the {@link String} representing the maven artifactId.
         * @return the builder to continue building.
         */
        public ReadmeHtmlVelocityDelegateBuilder withArtifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        /**
         * Adds the version to the {@link ReadmeHtmlVelocityDelegate}.
         * @param version the maven version.
         * @return the builder to continue building.
         */
        public ReadmeHtmlVelocityDelegateBuilder withVersion(String version) {
            this.version = version;
            return this;
        }

        /**
         * Adds the siteUrl to the {@link ReadmeHtmlVelocityDelegate}.
         * @param siteUrl the site url to be used in the <code>README.html</code>
         * @return the builder to continue building.
         */
        public ReadmeHtmlVelocityDelegateBuilder withSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
            return this;
        }

        /**
         * Builds up the {@link ReadmeHtmlVelocityDelegate} from the previously set parameters.
         * @return a new {@link ReadmeHtmlVelocityDelegate}.
         */
        public ReadmeHtmlVelocityDelegate build() {
            return new ReadmeHtmlVelocityDelegate(this.artifactId, this.version, this.siteUrl);
        }
    }
}
