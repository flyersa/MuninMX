/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package muninmxlictool;

import java.io.Serializable;

/**
 *
 * @author enricokern
 */
public class License implements  Serializable {
    private static long serialVersionUID = -4991349865504351284L;

    /**
     * @return the serialVersionUID
     */
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }


    private String licenseType = "base";
    private int num_nodes = 0;
    private int num_checks = 0;
    private long valid = 0;
    private String licenseID = "";
    

    /**
     * @return the licenseType
     */
    public String getLicenseType() {
        return licenseType;
    }

    /**
     * @param licenseType the licenseType to set
     */
    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    /**
     * @return the num_nodes
     */
    public int getNum_nodes() {
        return num_nodes;
    }

    /**
     * @param num_nodes the num_nodes to set
     */
    public void setNum_nodes(int num_nodes) {
        this.num_nodes = num_nodes;
    }

    /**
     * @return the valid
     */
    public long getValid() {
        return valid;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(long valid) {
        this.valid = valid;
    }

    /**
     * @return the licenseID
     */
    public String getLicenseID() {
        return licenseID;
    }

    /**
     * @param licenseID the licenseID to set
     */
    public void setLicenseID(String licenseID) {
        this.licenseID = licenseID;
    }

    /**
     * @return the num_checks
     */
    public int getNum_checks() {
        return num_checks;
    }

    /**
     * @param num_checks the num_checks to set
     */
    public void setNum_checks(int num_checks) {
        this.num_checks = num_checks;
    }
    
}
