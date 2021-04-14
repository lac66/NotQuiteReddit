// ForumsFragment.java
// Levi Carpenter

package com.learn.notquitereddit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ForumsFragment extends Fragment {
    private static String AUTH = "auth";
    Account currentUser;
    Button buttonForumLogout, buttonNewForum;

    ArrayList<Forum> forums;
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;
    ForumRecyclerViewAdapter adapter;
    ForumListListener fListener;

    public ForumsFragment() {
        // Required empty public constructor
    }

    public static ForumsFragment newInstance(Account account) {
        ForumsFragment fragment = new ForumsFragment();
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
        getActivity().setTitle("Forums");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_forums, container, false);

        // initialize elements
        buttonForumLogout = view.findViewById(R.id.buttonForumLogout);
        buttonNewForum = view.findViewById(R.id.buttonNewForum);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        forums = new ArrayList<>();

        getForumsList();

        adapter = new ForumRecyclerViewAdapter(forums);
        recyclerView.setAdapter(adapter);

        // listener for data from adapter
        adapter.setListener(new ForumRecyclerViewAdapter.IconClickListener() {
            @Override
            public void likeForum(String forumId) {
                likeToDb(forumId);
            }

            @Override
            public void unlikeForum(String forumId) {
                unlikeToDb(forumId);
            }

            @Override
            public void deleteForum(String forumId) {
                deleteForumFromDb(forumId);
            }

            @Override
            public void goToForum(Forum forum) {
                fListener.goToForum(currentUser, forum);
            }
        });

        // button listeners
        buttonForumLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fListener.goToLogin();
            }
        });

        buttonNewForum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fListener.goToNewForum(currentUser);
            }
        });

        return view;
    }

    // update list on fragment pop from backstack
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getForumsList();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof ForumListListener) {
            this.fListener = (ForumListListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement ForumListListener");
        }
    }

    interface ForumListListener {
        void goToNewForum(Account account);

        void goToLogin();

        void goToForum(Account account, Forum forum);
    }

    //
    // Database Methods
    //

    private void getForumsList() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        forums.clear();
                        for (QueryDocumentSnapshot document : value) {
                            String[] accountString = document.get("createdBy").toString().split(",");
                            String name = accountString[0].substring(6);
                            String id = accountString[1].substring(4);
                            String email = accountString[2].substring(7, accountString[2].length() - 1);
                            Account account = new Account(name, email, id);

                            HashSet<Account> likedBy = new HashSet<>();
                            if (document.contains("likedBy")) {
                                HashMap<String, Account> map = (HashMap) document.getData().get("likedBy");
                                for (Map.Entry mapElement : map.entrySet()) {
                                    String likedByUser = mapElement.getValue().toString();
                                    String[] splitUser = likedByUser.split(",");
                                    likedBy.add(new Account(splitUser[0].split("=")[1], splitUser[2].substring(7, splitUser[2].length() - 1), splitUser[1].split("=")[1]));
                                }
                            }

                            forums.add(new Forum(document.getId(), document.getString("title"), account, document.getTimestamp("createdAt"), document.getString("description"), likedBy));
                        }
                        Collections.sort(forums, new Comparator<Forum>() {
                            @RequiresApi(api = Build.VERSION_CODES.N)
                            @Override
                            public int compare(Forum o1, Forum o2) {
                                return Math.toIntExact(o2.getCreatedAtTimestamp().getSeconds() - o1.getCreatedAtTimestamp().getSeconds());
                            }
                        });
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void likeToDb(String forumId) {
        Forum forum = getForum(forumId);
        if (forum == null) {
            throw new NullPointerException();
        }
        forum.addLike(currentUser);

        HashMap<String, Object> forumMap = forumToHashMap(forum);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums").document(forumId).set(forumMap)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Liked", Toast.LENGTH_SHORT).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Forum Like Error")
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

    private void unlikeToDb(String forumId) {
        Forum forum = getForum(forumId);
        if (forum == null) {
            throw new NullPointerException();
        }
        forum.unlike(currentUser);

        HashMap<String, Object> forumMap = forumToHashMap(forum);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums").document(forumId).set(forumMap)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Unliked", Toast.LENGTH_SHORT).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Forum Like Error")
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

    Forum getForum(String id) {
        for (Forum forum: forums) {
            if (id.equals(forum.getForumId())) {
                return forum;
            }
        }
        return null;
    }

    HashMap<String, Object> forumToHashMap(Forum forum) {
        Log.d("TAG", "forumToHashMap: " + forum);
        Iterator<Account> iterator = forum.getLikedBy().iterator();
        HashMap<String, Object> likedByMap = new HashMap<>();
        while (iterator.hasNext()) {
            HashMap<String, Object> likedByAccountMap = new HashMap<>();
            Account account = iterator.next();
            likedByAccountMap.put("name", account.getName());
            likedByAccountMap.put("email", account.getEmail());
            likedByAccountMap.put("id", account.getId());
            likedByMap.put(account.getId(), likedByAccountMap);
        }

        HashMap<String, Object> forumMap = new HashMap<>();
        forumMap.put("title", forum.getTitle());
        forumMap.put("description", forum.getDescription());
        forumMap.put("createdBy", forum.getCreatedBy());
        forumMap.put("createdAt", forum.getCreatedAtTimestamp());
        forumMap.put("likedBy", likedByMap);

        return forumMap;
    }

    void deleteForumFromDb(String forumId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("forums").document(forumId).delete()
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            deleteForumComments(forumId);
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Forum Delete Error")
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

    void deleteForumComments(String forumId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("comments").document(forumId).delete()
                .addOnCompleteListener(getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Forum Deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Forum Delete Error")
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