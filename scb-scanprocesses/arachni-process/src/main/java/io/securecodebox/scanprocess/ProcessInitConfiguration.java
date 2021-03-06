/*
 *
 *  SecureCodeBox (SCB)
 *  Copyright 2015-2018 iteratec GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package io.securecodebox.scanprocess;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The secureCodeBox by default only scans for components in the package io.securecodebox.scanprocess.
 * <p>
 * This configuration ensures that your defined package io.securecodebox.scanprocesses also gets scanned, please don't move or remove this configuration.
 *
 * @author Rüdiger Heins - iteratec GmbH
 * @since 09.05.18
 */
@ComponentScan("io.securecodebox.scanprocesses")
@Configuration
public class ProcessInitConfiguration {
}
