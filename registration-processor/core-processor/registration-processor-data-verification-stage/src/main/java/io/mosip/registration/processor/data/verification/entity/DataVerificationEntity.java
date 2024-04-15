package io.mosip.registration.processor.data.verification.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;
@Entity
@Table(name = "reg_data_verification", schema = "regprc")
public class DataVerificationEntity implements Serializable{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The reg id. */
    @Id
    @Column(name = "reg_id")
    private String regId;

    /** The cr by. */
    @Column(name = "cr_by")
    private String crBy;

    /** The cr dtimes. */
    @Column(name = "cr_dtimes", updatable = false)
    private Timestamp crDtimes;

    /** The del dtimes. */
    @Column(name = "del_dtimes")
    private Timestamp delDtimes;

    /** The is active. */
    @Column(name = "is_active")
    private Boolean isActive;

    /** The is deleted. */
    @Column(name = "is_deleted")
    private Boolean isDeleted;


    /** The dv usr id. */
    @Column(name = "dv_usr_id")
    private String dvUsrId;

    /** The reason code. */
    @Column(name = "reason_code")
    private String reasonCode;

    /** The status code. */
    @Column(name = "status_code")
    private String statusCode;

    /** The status comment. */
    @Column(name = "status_comment")
    private String statusComment;

    /** The upd by. */
    @Column(name = "upd_by")
    private String updBy;

    /** The upd dtimes. */
    @Column(name = "upd_dtimes")
    private Timestamp updDtimes;

     @Column(name="request_id")
    private String requestId;


    /**
     * Sets the reg id.
     *
     * @param regId            the regId to set
     */
    public void setRegId(String regId) {
        this.regId = regId;
    }
    public String getRegId() {
        return regId;
    }

    /**
    /**
     * Gets the source name.
     *
     * @return the source name.
     */

    /**
     * Instantiates a new data verification entity.
     */
    public DataVerificationEntity() {
        super();
    }

    /**
     * Gets the cr by.
     *
     * @return the crBy
     */
    public String getCrBy() {
        return crBy;
    }

    /**
     * Sets the cr by.
     *
     * @param crBy
     *            the crBy to set
     */
    public void setCrBy(String crBy) {
        this.crBy = crBy;
    }

    /**
     * Gets the cr dtimes.
     *
     * @return the crDtimes
     */
    public Timestamp getCrDtimes() {
        return new Timestamp(crDtimes.getTime());
    }

    /**
     * Sets the cr dtimes.
     *
     * @param crDtimes
     *            the crDtimes to set
     */
    public void setCrDtimes(Timestamp crDtimes) {
        this.crDtimes = new Timestamp(crDtimes.getTime());
    }

    /**
     * Gets the del dtimes.
     *
     * @return the delDtimes
     */
    public Timestamp getDelDtimes() {
        return new Timestamp(delDtimes.getTime());
    }

    /**
     * Sets the del dtimes.
     *
     * @param delDtimes
     *            the delDtimes to set
     */
    public void setDelDtimes(Timestamp delDtimes) {
        this.delDtimes = new Timestamp(delDtimes.getTime());
    }

    /**
     * Gets the checks if is active.
     *
     * @return the isActive
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets the checks if is active.
     *
     * @param isActive
     *            the isActive to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Gets the checks if is deleted.
     *
     * @return the isDeleted
     */
    public Boolean getIsDeleted() {
        return isDeleted;
    }

    /**
     * Sets the checks if is deleted.
     *
     * @param isDeleted
     *            the isDeleted to set
     */
    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }


    /**
     * Gets the dv usr id.
     *
     * @return the dvUsrId
     */
    public String getDvUsrId() {
        return dvUsrId;
    }

    /**
     * Sets the mv usr id.
     *
     * @param dvUsrId
     *            the mvUsrId to set
     */
    public void setDvUsrId(String dvUsrId) {
        this.dvUsrId = dvUsrId;
    }

    /**
     * Gets the reason code.
     *
     * @return the reasonCode
     */
    public String getReasonCode() {
        return reasonCode;
    }

    /**
     * Sets the reason code.
     *
     * @param reasonCode
     *            the reasonCode to set
     */
    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    /**
     * Gets the status code.
     *
     * @return the statusCode
     */
    public String getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the status code.
     *
     * @param statusCode
     *            the statusCode to set
     */
    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the status comment.
     *
     * @return the statusComment
     */
    public String getStatusComment() {
        return statusComment;
    }

    /**
     * Sets the status comment.
     *
     * @param statusComment
     *            the statusComment to set
     */
    public void setStatusComment(String statusComment) {
        this.statusComment = statusComment;
    }

    /**
     * Gets the upd by.
     *
     * @return the updBy
     */
    public String getUpdBy() {
        return updBy;
    }

    /**
     * Sets the upd by.
     *
     * @param updBy
     *            the updBy to set
     */
    public void setUpdBy(String updBy) {
        this.updBy = updBy;
    }

    /**
     * Gets the upd dtimes.
     *
     * @return the updDtimes
     */
    public Timestamp getUpdDtimes() {
        return new Timestamp(updDtimes.getTime());
    }

    /**
     * Sets the upd dtimes.
     *
     * @param updDtimes
     *            the updDtimes to set
     */
    public void setUpdDtimes(Timestamp updDtimes) {
        this.updDtimes = new Timestamp(updDtimes.getTime());
    }

    /**
     * Gets the serialversionuid.
     *
     * @return the serialversionuid
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }


    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }



}
