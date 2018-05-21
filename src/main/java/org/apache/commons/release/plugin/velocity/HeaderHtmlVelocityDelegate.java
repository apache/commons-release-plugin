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
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * This class' purpose is to generate the <code>HEADER.html</code> that moves along with the
 * release for the sake of downloading the release from the distribution area.
 *
 * @author chtompki
 * @since 1.3
 */
public class HeaderHtmlVelocityDelegate {
    /** The location of the velocity tempate for this class. */
    private static final String TEMPLATE = "resources/org/apache/commons/release/plugin"
                                         + "/velocity/HEADER.vm";
    /** The private constructor to be used by the {@link HeaderHtmlVelocityDelegateBuilder}. */
    private HeaderHtmlVelocityDelegate() {
        super();
    }

    /**
     * For instantiating our {@link HeaderHtmlVelocityDelegate} using the {@link HeaderHtmlVelocityDelegateBuilder}.
     *
     * @return a {@link HeaderHtmlVelocityDelegateBuilder}.
     */
    public static HeaderHtmlVelocityDelegateBuilder builder() {
        return new HeaderHtmlVelocityDelegateBuilder();
    }

    /**
     * Builds the HEADER.vm velocity template to the writer passed in.
     *
     * @param writer any {@link Writer} that we wish to have the filled velocity template written to.
     * @return the {@link Writer} that we've filled out the template into.
     */
    public Writer render(Writer writer) {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        Template template = ve.getTemplate(TEMPLATE);
        VelocityContext context = new VelocityContext();
        template.merge(context, writer);
        return writer;
    }

    /**
     * A builder class for instantiation of the {@link HeaderHtmlVelocityDelegate}.
     */
    public static class HeaderHtmlVelocityDelegateBuilder {

        /**
         * Private constructor so that we can have a proper builder pattern.
         */
        private HeaderHtmlVelocityDelegateBuilder() {
            super();
        }

        /**
         * Builds up the {@link ReadmeHtmlVelocityDelegate} from the previously set parameters.
         * @return a new {@link ReadmeHtmlVelocityDelegate}.
         */
        public HeaderHtmlVelocityDelegate build() {
            return new HeaderHtmlVelocityDelegate();
        }
    }
}
