package com.koushikdutta.googlevoice;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(detachable = "true")
public class Registration {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;
    public Long getId() {
        return id;
    }
    
    @Persistent
    private String clientId;
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    @Persistent
    private Integer unreadTextCount;
    public Integer getUnreadTextCount() {
        return unreadTextCount;
    }
    public void setUnreadTextCount(Integer unreadTextCount) {
        this.unreadTextCount = unreadTextCount;
    }
    
    @Persistent
    private Integer unreadVoicemailCount;
    public Integer getUnreadVoicemailCount() {
        return unreadVoicemailCount;
    }
    public void setUnreadVoicemailCount(Integer unreadVoicemailCount) {
        this.unreadVoicemailCount = unreadVoicemailCount;
    }
}
