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
package org.apache.commons.release.plugin.handler;

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

import java.io.File;

@Component(role= DistributionScmHandler.class, instantiationStrategy = "singleton" )
public class DistributionScmHandler extends AbstractLogEnabled {

    /**
     * The SCM manager.
     */
    @Requirement
    private ScmManager scmManager;

    /**
     * When this plugin requires Maven 3.0 as minimum, this component can be removed and o.a.m.s.c.SettingsDecrypter be
     * used instead.
     */
    @Requirement(hint = "mng-4384")
    private SecDispatcher secDispatcher;

    public void checkoutDirectory(String scmUrl, File checkoutRootDirectory) {

    }

    public ScmRepository getConfiguredRepository(String url,
                                                 String username,
                                                 String password,
                                                 String privateKey,
                                                 String passphrase,
                                                 Settings settings)
            throws ScmRepositoryException, NoSuchScmProviderException {
        ScmRepository repository = scmManager.makeScmRepository(url);
        ScmProviderRepository scmRepo = repository.getProviderRepository();
        //MRELEASE-76
        scmRepo.setPersistCheckout(false);
        if (settings != null) {
            Server server = null;
            if (server == null && repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost) {
                ScmProviderRepositoryWithHost repositoryWithHost =
                        (ScmProviderRepositoryWithHost) repository.getProviderRepository();
                String host = repositoryWithHost.getHost();
                int port = repositoryWithHost.getPort();
                if (port > 0) {
                    host += ":" + port;
                }
                // TODO: this is a bit dodgy - id is not host, but since we don't have a <host> field we make an assumption
                server = settings.getServer(host);
            }

            if (server != null) {
                if (username == null) {
                    username = server.getUsername();
                }
                if (password == null) {
                    password = decrypt(server.getPassword(), server.getId());
                }
                if (privateKey == null) {
                    privateKey = server.getPrivateKey();
                }
                if (passphrase == null) {
                    passphrase = decrypt(server.getPassphrase(), server.getId());
                }
            }
        }
        if (!StringUtils.isEmpty(username)) {
            scmRepo.setUser(username);
        }
        if (!StringUtils.isEmpty(password)) {
            scmRepo.setPassword(password);
        }
        if (scmRepo instanceof ScmProviderRepositoryWithHost) {
            ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
            if (!StringUtils.isEmpty(privateKey)) {
                repositoryWithHost.setPrivateKey(privateKey);
            }
            if (!StringUtils.isEmpty(passphrase)) {
                repositoryWithHost.setPassphrase(passphrase);
            }
        }
        return repository;
    }

    private String decrypt(String str, String server) {
        try {
            return secDispatcher.decrypt(str);
        } catch (SecDispatcherException e) {
            String msg =
                    "Failed to decrypt password/passphrase for server " + server + ", using auth token as is: "
                            + e.getMessage();
            if (getLogger().isDebugEnabled()) {
                getLogger().warn(msg, e);
            } else {
                getLogger().warn(msg);
            }
            return str;
        }
    }

}
