/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
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
