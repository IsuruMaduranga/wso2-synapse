/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.transport.dynamicconfigurations;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.ParameterInclude;

public class KeyStoreReloader {

    private IKeyStoreLoader keyStoreLoader;
    private ParameterInclude transportOutDescription;

    public KeyStoreReloader(IKeyStoreLoader keyStoreLoader, ParameterInclude transportOutDescription) {

        this.keyStoreLoader = keyStoreLoader;
        this.transportOutDescription = transportOutDescription;

        registerListener(transportOutDescription);
    }

    private void registerListener(ParameterInclude transportOutDescription) {

        KeyStoreReloaderHolder.getInstance().addKeyStoreLoader(this);
    }

    public void update() {

        try {
            keyStoreLoader.loadKeyStore(transportOutDescription);
        } catch (AxisFault e) {
            throw new RuntimeException(e);
        }
    }
}
