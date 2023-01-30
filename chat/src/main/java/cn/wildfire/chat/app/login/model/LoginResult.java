/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.login.model;


/**
 * 真-model，code啊，message之类的，放到了status里面去了
 */
public class LoginResult {
    private String userId;
    private String token;
    private boolean register;
    private String resetCode;
    private boolean createGroupEnable;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }

    public String getResetCode() {
        return resetCode;
    }

    public void setResetCode(String resetCode) {
        this.resetCode = resetCode;
    }

    public boolean isCreateGroupEnable() {
        return createGroupEnable;
    }

    public void setCreateGroupEnable(boolean createGroupEnable) {
        this.createGroupEnable = createGroupEnable;
    }
}
