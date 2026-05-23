<!---
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
# Attestation examples

Golden files used as expected values by the tests.

Real build attestations are single-line JSON Lines (`.intoto.jsonl`). The files here are pretty-printed JSON (`.intoto.json`) for readability.

The tests compare structurally via `json-unit`, ignoring environment-dependent fields (JVM arguments, timestamps, `resolvedDependencies` digests), so formatting differences are not significant.