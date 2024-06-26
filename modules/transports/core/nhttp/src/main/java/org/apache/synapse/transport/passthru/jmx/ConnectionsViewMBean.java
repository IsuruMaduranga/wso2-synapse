/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.apache.synapse.transport.passthru.jmx;

import java.util.Date;
import java.util.Map;

public interface ConnectionsViewMBean {

    public int getActiveConnections();
    public int getLastSecondRequests();
    public int getLast15SecondRequests();
    public int getLastMinuteRequests();
    public int getLastSecondConnections();
    public int getLast5SecondConnections();
    public int getLast15SecondConnections();
    public int getLastMinuteConnections();
    public int getLast5MinuteConnections();
    public int getLast15MinuteConnections();
    public int getLastHourConnections();
    public int getLast8HourConnections();
    public int getLast24HourConnections();
    public Map getRequestSizesMap();
    public Map getResponseSizesMap();
    public Date getLastResetTime();

    public void reset();

}

