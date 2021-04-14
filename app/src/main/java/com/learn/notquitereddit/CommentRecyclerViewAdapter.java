// CommentRecyclerViewAdapter.java
// Levi Carpenter

package com.learn.notquitereddit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class CommentRecyclerViewAdapter extends RecyclerView.Adapter<CommentRecyclerViewAdapter.CommentViewHolder> {
    private FirebaseAuth mAuth;
    ArrayList<Comment> comments;
    Forum forum;
    ClickListener cListener;

    public CommentRecyclerViewAdapter(ArrayList<Comment> data, Forum forum) {
        this.comments = data;
        this.forum = forum;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_row_item, parent, false);
        CommentViewHolder commentViewHolder = new CommentViewHolder(view);

        return commentViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.comment = comment;
        holder.textViewCommentCreator.setText(comment.getCreatedBy().getName());
        holder.textViewComment.setText(comment.getText());
        holder.textViewCommentDate.setText(comment.getCreatedAt());

        // hide delete icon if current user is not comment owner
        if (!mAuth.getCurrentUser().getUid().equals(comment.getCreatedBy().getId())) {
            holder.imageViewDeleteComment.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return this.comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewCommentCreator, textViewComment, textViewCommentDate;
        ImageView imageViewDeleteComment;
        Comment comment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewCommentCreator = itemView.findViewById(R.id.textViewCommentCreator);
            textViewComment = itemView.findViewById(R.id.textViewComment);
            textViewCommentDate = itemView.findViewById(R.id.textViewCommentDate);
            imageViewDeleteComment = itemView.findViewById(R.id.imageViewDeleteComment);

             imageViewDeleteComment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cListener.deleteComment(forum.getForumId(), comment.getCommentId());
                }
            });
        }
    }

    void setListener(ClickListener listener) {
        this.cListener = listener;
    }

    interface ClickListener {
        void deleteComment(String forumId, String commentId);
    }
}
