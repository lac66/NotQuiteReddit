// NewForumFragment.java
// Levi Carpenter

package com.learn.notquitereddit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class NewForumFragment extends Fragment {
    private static final String AUTH = "AUTH";

    private Account currentUser;
    EditText editTextNewForumTitle, editTextNewForumDescription;
    Button buttonNewForumSubmit, buttonNewForumCancel;
    NewForumListener nListener;

    public NewForumFragment() {
        // Required empty public constructor
    }

    public static NewForumFragment newInstance(Account account) {
        NewForumFragment fragment = new NewForumFragment();
        Bundle args = new Bundle();
        args.putSerializable(AUTH, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUser = (Account) getArguments().getSerializable(AUTH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_new_forum, container, false);

        // initialize elements
        editTextNewForumTitle = view.findViewById(R.id.editTextNewForumTitle);
        editTextNewForumDescription = view.findViewById(R.id.editTextNewForumDescription);
        buttonNewForumSubmit = view.findViewById(R.id.buttonNewForumSubmit);
        buttonNewForumCancel = view.findViewById(R.id.buttonNewForumCancel);

        // button listeners
        buttonNewForumSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = editTextNewForumTitle.getText().toString();
                String description = editTextNewForumDescription.getText().toString();
                if (title.isEmpty()) {
                    Toast.makeText(getActivity(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                } else if (description.isEmpty()) {
                    Toast.makeText(getActivity(), "Description cannot be empty", Toast.LENGTH_SHORT).show();
                } else {
                    createNewForum(title, description);
                }
            }
        });

        buttonNewForumCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nListener.goToForumsFromNewForum();
            }
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof NewForumListener) {
            this.nListener = (NewForumListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NewForumListener");
        }
    }

    interface NewForumListener {
        void goToForumsFromNewForum();
    }

    //
    // Database Methods
    //

    void createNewForum(String title, String description) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> forum = new HashMap<>();
        forum.put("title", title);
        forum.put("description", description);
        forum.put("createdBy", currentUser);
        forum.put("createdAt", Calendar.getInstance().getTime());
        forum.put("likedBy", new HashMap<String, Object>());

        db.collection("forums").add(forum)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Forum Created", Toast.LENGTH_SHORT).show();
                            createCommentSection(task.getResult().getId());
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("New Forum Error")
                                    .setMessage(task.getException().getMessage())
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });
                            builder.create().show();
                        }
                    }
                });
    }

    void createCommentSection(String forumId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("forumComments", new ArrayList<Comment>());

        db.collection("comments").document(forumId).set(hashMap)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            nListener.goToForumsFromNewForum();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("New Forum Error")
                                    .setMessage(task.getException().getMessage())
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });
                            builder.create().show();
                        }
                    }
                });
    }
}