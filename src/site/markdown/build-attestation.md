<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
# commons-release:build-attestation

## Overview

The `commons-release:build-attestation` goal produces a [SLSA](https://slsa.dev/) v1.2 provenance
statement in the [in-toto](https://in-toto.io/) format. The attestation lists every artifact
attached to the project as a subject, records the JDK, Maven installation, SCM source and
resolved dependencies used during the build, and writes the result to
`target/<artifactId>-<version>.intoto.jsonl`. The envelope is signed with GPG by default and
attached to the project so that it is deployed alongside the other artifacts.

The structure of the `predicate.buildDefinition.buildType` field is documented at
[SLSA build type v0.1.0](slsa/v0.1.0.html).

## Phase ordering

A Commons release relies on three goals running in a fixed order:

1. `commons-release:build-attestation`, bound to `post-integration-test`. At this point every
   build artifact, including the distribution archives, is already attached to the project.
2. `maven-gpg-plugin:sign`, bound to `verify`. It signs every attached artifact with a detached
   `.asc`, including the `.intoto.jsonl` produced in step 1. Maven Central requires this for
   every uploaded file.
3. `commons-release:detach-distributions`, bound to `verify`. It removes the `.tar.gz` and
   `.zip` archives from the set of artifacts that will be uploaded to Nexus.

Binding `build-attestation` to `post-integration-test` (rather than `verify`) puts it in an
earlier lifecycle phase than the other two goals, so Maven 3 is guaranteed to run it first,
regardless of the order in which plugins are declared in the POM. Within the `verify` phase,
`sign` must run before `detach-distributions`; this is controlled by declaring
`maven-gpg-plugin` before `commons-release-plugin` in the POM, since Maven executes plugins
within a single phase in the order they appear.

If the distribution archives should not be covered by the attestation, override the default
phase binding and bind `build-attestation` to `verify` after `detach-distributions`.

## Example configuration

The snippet below wires the three goals in the recommended order.

```xml
<build>
  <plugins>
    <!--
      maven-gpg-plugin is declared first, so its `sign` execution runs first within the
      `verify` phase, before `detach-distributions`.
    -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-gpg-plugin</artifactId>
      <executions>
        <execution>
          <id>sign-artifacts</id>
          <phase>verify</phase>
          <goals>
            <goal>sign</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    <plugin>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-release-plugin</artifactId>
      <executions>
        <!--
          `build-attestation` uses its default phase `post-integration-test`, which runs
          before `verify` and therefore before both `sign` and `detach-distributions`.
        -->
        <execution>
          <id>build-attestation</id>
          <goals>
            <goal>build-attestation</goal>
          </goals>
        </execution>
        <execution>
          <id>detach-distributions</id>
          <phase>verify</phase>
          <goals>
            <goal>detach-distributions</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

See the [goal parameters](build-attestation-mojo.html) for the full list of configurable
properties.
