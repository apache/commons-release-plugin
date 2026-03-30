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

import java.nio.file.Path;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;

/**
 * Utilities to instantiate Mojos in a test environment.
 */
public final class MojoUtils {

    private static ContainerConfiguration setupContainerConfiguration() {
        ClassWorld classWorld =
                new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
        return new DefaultContainerConfiguration()
                .setClassWorld(classWorld)
                .setClassPathScanning(PlexusConstants.SCANNING_INDEX)
                .setAutoWiring(true)
                .setName("maven");
    }

    public static PlexusContainer setupContainer() throws PlexusContainerException {
        return new DefaultPlexusContainer(setupContainerConfiguration());
    }

    public static RepositorySystemSession createRepositorySystemSession(
            PlexusContainer container, Path localRepositoryPath) throws ComponentLookupException, RepositoryException {
        LocalRepositoryManagerFactory factory = container.lookup(LocalRepositoryManagerFactory.class, "simple");
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession();
        LocalRepositoryManager manager =
                factory.newInstance(repoSession, new LocalRepository(localRepositoryPath.toFile()));
        repoSession.setLocalRepositoryManager(manager);
        // Default policies
        repoSession.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
        repoSession.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        return repoSession;
    }

    private MojoUtils() {
    }
}
