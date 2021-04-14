// ForumFragment.java
// Levi Carpenter

package com.learn.notquitereddit;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class ForumFragment extends Fragment {
    private static final String ACC = "acc";
    private static final String FORUM = "FORUM";

    private Forum forum;
    private Account currentUser;

    TextView textViewForumTitle, textViewForumCreator, textViewForumDescription, textViewNumComments;
    EditText editTextWriteComment;
    Button buttonPostComment;

    ArrayList<Comment> comments;
    RecyclerView recyclerViewComments;
    LinearLayoutManager layoutManager;
    CommentRecyclerViewAdapter adapter;

    public ForumFragment() {
        // Required empty public constructor
    }

    public static ForumFragment newInstance(Account account, Forum forum) {
        ForumFragment fragment = new ForumFragment();
        Bundle args = new Bundle();
        args.putSerializable(ACC, account);
        args.putSerializable(FORUM, forum);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUser = (Account) getArguments().getSerializable(ACC);
            forum = (Forum) getArguments().getSerializable(FORUM);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getActivity().setTitle("Forum");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_forum, container, false);

        // initialize elements
        textViewForumTitle = view.findViewById(R.id.textViewForumTitle);
        textViewForumCreator = view.findViewById(R.id.textViewForumCreator);
        textViewForumDescription = view.findViewById(R.id.textViewForumDescription);
        textViewNumComments = view.findViewById(R.id.textViewNumComments);
        editTextWriteComment = view.findViewById(R.id.editTextWriteComment);
        buttonPostComment = view.findViewById(R.id.buttonPostComment);

        textViewForumDescription.setMaxLines(8);
        textViewForumDescription.setVerticalScrollBarEnabled(true);
        textViewForumTitle.setText(forum.getTitle());
        textViewForumCreator.setText(forum.getCreatedBy().getName());
        textViewForumDescription.setText(forum.getDescription());

        recyclerViewComments = view.findViewById(R.id.recyclerViewComments);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerViewComments.setLayoutManager(layoutManager);

        comments = new ArrayList<>();

        getComments(forum);

        adapter = new CommentRecyclerViewAdapter(comments, forum);
        recyclerViewComments.setAdapter(adapter);
        textViewNumComments.setText(comments.size() + " Comments");

        adapter.setListener(new CommentRecyclerViewAdapter.ClickListener() {
            @Override
            public void deleteComment(String forumId, String commentId) {
                deleteCommentFromDb(forumId, commentId);
            }
        });

        buttonPostComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = editTextWriteComment.getText().toString();
                if (!comment.isEmpty()) {
                    createComment(forum.getForumId(), comment);
                    editTextWriteComment.setText("");
                }
            }
        });

        return view;
    }

    //
    // Database methods
    //

    public void getComments(Forum forum) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("comments").document(forum.getForumId())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        comments.clear();
                        List<HashMap<String, Object>> commentList = (List<HashMap<String, Object>>) value.get("forumComments");

                        for (HashMap<String, Object> hashMap: commentList) {
                            String forumId = forum.getForumId();
                            String commentId = (String) hashMap.get("commentId");
                            String text = (String) hashMap.get("text");
                            HashMap<String, Object> accountHashMap = (HashMap<String, Object>) hashMap.get("createdBy");
                            String accountName = (String) accountHashMap.get("name");
                            String accountEmail = (String) accountHashMap.get("email");
                            String accountId = (String) accountHashMap.get("id");
                            Account createdBy = new Account(accountName, accountEmail, accountId);
                            Timestamp createdAt = (Timestamp) hashMap.get("createdAtTimestamp");

                            Comment comment = new Comment(forumId, commentId, text, createdBy, createdAt);

                            comments.add(comment);
                        }
                        Collections.sort(comments, new Comparator<Comment>() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public int compare(Comment o1, Comment o2) {
                                return Math.toIntExact(o1.getCreatedAtTimestamp().getSeconds() - o2.getCreatedAtTimestamp().getSeconds());
                            }
                        });
                        adapter.notifyDataSetChanged();
                        updateCommentCount();
                    }
                });
    }

    public void deleteCommentFromDb(String forumId, String commentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Comment comment = null;
        for (Comment tmp : comments) {
            if (commentId.equals(tmp.getCommentId())) {
                comment = tmp;
            }
        }

        if (comment == null) {
            throw new NullPointerException();
        }

        comments.remove(comment);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("forumComments", comments);


        db.collection("comments").document(forumId)
                .set(hashMap).addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateCommentCount();
                } else {
                    Log.d("TAG", "onComplete: Fail");
                }
            }
        });
    }

    public void createComment(String forumId, String text) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Random rand = new Random();
        String commentId = "";
        for (int i = 0; i < 20; i++) {
            int randomInt = rand.nextInt(61);
            if (randomInt < 10) {
                commentId += Character.toString((char)(randomInt + 48));
            } else if (randomInt >= 10 && randomInt < 36) {
                commentId += Character.toString((char)(randomInt + 55));
            } else {
                commentId += Character.toString((char)(randomInt + 61));
            }
        }
        comments.add(new Comment(forumId, commentId, text, currentUser, Timestamp.now()));

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("forumComments", comments);


        db.collection("comments").document(forumId)
                .set(hashMap).addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    updateCommentCount();
                } else {
                    Log.d("TAG", "onComplete: Fail");
                }
            }
        });
    }

    void updateCommentCount() {
        textViewNumComments.setText(comments.size() + " Comments");
    }
}