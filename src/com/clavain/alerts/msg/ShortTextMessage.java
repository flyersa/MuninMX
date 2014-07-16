/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.alerts.msg;

/**
 *
 * @author enricokern
 */
public class ShortTextMessage {
    private String message;
    private String mobile;
    
    public ShortTextMessage(String p_message, String p_mobile)
    {
        message = p_message;
        mobile = p_mobile;
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

    /**
     * @return the mobile
     */
    public String getMobile() {
        return mobile;
    }

    /**
     * @param mobile the mobile to set
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}

