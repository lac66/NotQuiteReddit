// Comment.java
// Levi Carpenter

// class to manage comment objects

package com.learn.notquitereddit;

import android.text.format.DateFormat;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.Calendar;

public class Comment implements Serializable {
    String text;
    Account createdBy;
    String createdAt;
    String commentId;
    String forumId;
    Timestamp createdAtTimestamp;

    public Comment(String forumId, String commentId, String text, Account createdBy, Timestamp createdAt) {
        this.text = text;
        this.createdBy = createdBy;
        this.createdAtTimestamp = createdAt;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(createdAt.getSeconds() * 1000L);
        this.createdAt = DateFormat.format("MM-dd-yyyy hh:mm a", cal).toString();
        this.commentId = commentId;
        this.forumId = forumId;
    }

    public String getForumId() { return forumId; }

    public String getCommentId() {
        return commentId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Account getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Account createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Timestamp getCreatedAtTimestamp() {
        return createdAtTimestamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "text='" + text + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", commentId=" + commentId +
                '}';
    }
}
