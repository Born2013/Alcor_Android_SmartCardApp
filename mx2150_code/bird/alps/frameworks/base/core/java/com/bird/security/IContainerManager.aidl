package com.bird.security;

/** @hide */
interface IContainerManager {

    boolean addDefaultSecurityUser();

    String getData(String str);

    boolean isContainerJustRemoved();

    boolean isCurrentContainerUser();

    boolean isUserRemoving(int i);

    void resetContainer();
    
    void destroyContainer();

    void setData(String str, String str2);

    void stopContainer();

    boolean switchToSecurityContainer();

    void switchToZEROUserContainer();
    
    void reportFailedPasswordAttempt();
    
    void reportSuccessfulPasswordAttempt();
    
    int getPasswordAttemptRemainingTimes();
}
