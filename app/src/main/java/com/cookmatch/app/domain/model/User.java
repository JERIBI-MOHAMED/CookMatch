package com.cookmatch.app.domain.model;

import java.util.ArrayList;
import java.util.List;

public class User {

    private String uid;
    private String email;
    private List<String> savedRecipeIds;

    public User() {
        this.savedRecipeIds = new ArrayList<>();
    }

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
        this.savedRecipeIds = new ArrayList<>();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<String> getSavedRecipeIds() { return savedRecipeIds; }
    public void setSavedRecipeIds(List<String> ids) { this.savedRecipeIds = ids; }
}
