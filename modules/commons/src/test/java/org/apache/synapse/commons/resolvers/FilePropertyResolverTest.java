/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.resolvers;

import junit.framework.TestCase;
import org.apache.synapse.commons.util.FilePropertyLoader;

public class FilePropertyResolverTest extends TestCase {

    /**
     * Test file property resolve method
     */
    public void testResolve() {
        String inputValue = "testKey";
        FilePropertyLoader propertyLoader = FilePropertyLoader.getInstance();
        System.setProperty("conf.location", System.getProperty("user.dir") + "/src/test/resources/");
        propertyLoader.loadPropertiesFile();
        String filePropertyValue = propertyLoader.getValue(inputValue);
        assertEquals("Couldn't resolve the file property variable", "testValue", filePropertyValue);
    }
}
