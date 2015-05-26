/*
 * MuninMX
 * Written by Enrico Kern, kern@clavain.com
 * www.clavain.com
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.clavain.alerts.msg;

/**
 *
 * @author enricokern
 */
public class PushOverMessage {
    private String userKey;
    private String title;
    private String message;
    
    public PushOverMessage(String p_userKey, String p_title, String p_message)
    {
        userKey = p_userKey;
        title = p_title;
        message = p_message;
    }

    /**
     * @return the userKey
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * @param userKey the userKey to set
     */
    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
