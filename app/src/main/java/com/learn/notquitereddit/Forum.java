// Forum.java
// Levi Carpenter

// class to manage forum objects

package com.learn.notquitereddit;

import android.text.format.DateFormat;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashSet;

public class Forum implements Serializable {
    private String title;
    private Account createdBy;
    private String createdAt;
    private String description;
    private HashSet<Account> likedBy;
    private String forumId;
    private Timestamp createdAtTimestamp;

    public Forum(String forumId, String title, Account createdBy, Timestamp createdAt, String description, HashSet<Account> likedBy) {
        this.title = title;
        this.createdBy = createdBy;
        this.createdAtTimestamp = createdAt;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(createdAt.getSeconds() * 1000L);
        this.createdAt = DateFormat.format("MM-dd-yyyy hh:mm a", cal).toString();
        this.description = description;
        if (likedBy == null) {
            this.likedBy = new HashSet<>();
        } else {
            this.likedBy = likedBy;
        }
        this.forumId = forumId;
    }

    public String getForumId() {
        return forumId;
    }

    public String getTitle() {
        return title;
    }

    public Account getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Timestamp getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }

    public String getDescription() {
        return description;
    }

    public HashSet<Account> getLikedBy() {
        return likedBy;
    }

    public void addLike(Account account) {
        this.likedBy.add(account);
    }

    public void unlike(Account account) {
        this.likedBy.remove(account);
    }

    @Override
    public String toString() {
        return "Forum{" +
                "title='" + title + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", description='" + description + '\'' +
                ", likedBy=" + likedBy +
                ", forumId=" + forumId +
                '}';
    }
}
