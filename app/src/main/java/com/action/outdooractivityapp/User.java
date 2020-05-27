package com.action.outdooractivityapp;

public class User {
    private String userId;
    private String userPassWord;
    private String nickName;
    private String profile_image;

    public User(String userId, String userPassWord, String nickName, String profile_image) {
        this.userId = userId;
        this.userPassWord = userPassWord;
        this.nickName = nickName;
        this.profile_image = profile_image;
    }

    public User(String userId, String userPassWord, String nickName) {
        this.userId = userId;
        this.userPassWord = userPassWord;
        this.nickName = nickName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPassWord() {
        return userPassWord;
    }

    public void setUserPassWord(String userPassWord) {
        this.userPassWord = userPassWord;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(String profile_image) {
        this.profile_image = profile_image;
    }
}
