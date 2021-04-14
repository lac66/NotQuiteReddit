// ForumRecyclerViewAdapter.java
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

public class ForumRecyclerViewAdapter extends RecyclerView.Adapter<ForumRecyclerViewAdapter.ForumViewHolder> {
    FirebaseAuth mAuth;
    ArrayList<Forum> forums;
    IconClickListener icListener;

    public ForumRecyclerViewAdapter(ArrayList<Forum> data) {
        this.forums = data;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public ForumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.forum_row_item, parent, false);
        ForumViewHolder forumViewHolder = new ForumViewHolder(view);

        return forumViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ForumViewHolder holder, int position) {
        Forum forum = forums.get(position);
        holder.forum = forum;
        holder.textViewTitle.setText(forum.getTitle());
        holder.textViewCreator.setText(forum.getCreatedBy().getName());
        holder.textViewDescription.setText(forum.getDescription());
        holder.textViewLikes.setText(forum.getLikedBy().size() + " Likes");
        holder.textViewDate.setText(forum.getCreatedAt());

        // hide delete icon if current user is not forum owner
        if (!forum.getCreatedBy().getId().equals(mAuth.getCurrentUser().getUid())) {
            holder.imageViewTrash.setVisibility(View.INVISIBLE);
        }

        // set like icon accordingly
        if (forum.getLikedBy().contains(new Account(mAuth.getCurrentUser().getUid()))) {
            holder.liked = true;
            holder.imageViewLike.setImageResource(R.drawable.like_favorite);
        }
    }

    @Override
    public int getItemCount() {
        return this.forums.size();
    }

    class ForumViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewCreator, textViewDescription, textViewLikes, textViewDate;
        ImageView imageViewLike, imageViewTrash;
        boolean liked;
        Forum forum;

        public ForumViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewCreator = itemView.findViewById(R.id.textViewCreator);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
            textViewLikes = itemView.findViewById(R.id.textViewLikes);
            textViewDate = itemView.findViewById(R.id.textViewDate);
            imageViewLike = itemView.findViewById(R.id.imageViewLike);
            imageViewTrash = itemView.findViewById(R.id.imageViewTrash);

            imageViewLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (liked) {
                        icListener.unlikeForum(forum.getForumId());
                        imageViewLike.setImageResource(R.drawable.like_not_favorite);
                        liked = false;
                    } else {
                        icListener.likeForum(forum.getForumId());
                        imageViewLike.setImageResource(R.drawable.like_favorite);
                        liked = true;
                    }
                }
            });

            imageViewTrash.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    icListener.deleteForum(forum.getForumId());
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    icListener.goToForum(forum);
                }
            });
        }
    }

    void setListener(IconClickListener listener) {
        this.icListener = listener;
    }

    interface IconClickListener {
        void likeForum(String forumId);
        void unlikeForum(String forumId);
        void deleteForum(String forumId);
        void goToForum(Forum forum);
    }
}
