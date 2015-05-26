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
package com.clavain.json;

/**
 *
 * @author enricokern
 */
public class User {
    private Integer user_id;
    private String username;
    private String accessgroup;
    private String userrole;

    /**
     * @return the user_id
     */
    public Integer getUser_id() {
        return user_id;
    }

    /**
     * @param user_id the user_id to set
     */
    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the accessgroup
     */
    public String getAccessgroup() {
        return accessgroup;
    }

    /**
     * @param accessgroup the accessgroup to set
     */
    public void setAccessgroup(String accessgroup) {
        this.accessgroup = accessgroup;
    }

    /**
     * @return the userrole
     */
    public String getUserrole() {
        return userrole;
    }

    /**
     * @param userrole the userrole to set
     */
    public void setUserrole(String userrole) {
        this.userrole = userrole;
    }

}
