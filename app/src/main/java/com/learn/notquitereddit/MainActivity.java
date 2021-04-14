// MainActivity.java
// Levi Carpenter

// Fragment manager

package com.learn.notquitereddit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity implements LoginFragment.LoginListener, RegistrationFragment.RegistrationListener, ForumsFragment.ForumListListener, NewForumFragment.NewForumListener {
    FirebaseAuth mAuth;
    Account currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            setCurrentUserAndGoToForums();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.containerView, new LoginFragment())
                    .commit();
        }
    }

    // create user object if already logged in
    void setCurrentUserAndGoToForums() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("accounts").get()
                .addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document: task.getResult()) {
                                Log.d("TAG", "onComplete: in for loop");
                                if (mAuth.getCurrentUser().getUid().equals(document.getId())) {
                                    currentUser = new Account(document.getString("name"), document.getString("email"), document.getId());

                                    getSupportFragmentManager().beginTransaction()
                                            .add(R.id.containerView, ForumsFragment.newInstance(currentUser))
                                            .commit();
                                }
                            }
                        } else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Login Error")
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

    @Override
    public void goToRegistration() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerView, new RegistrationFragment())
                .commit();
    }

    @Override
    public void goToNewForum(Account account) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerView, NewForumFragment.newInstance(account))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goToLogin() {
        mAuth.signOut();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerView, new LoginFragment())
                .commit();
    }

    @Override
    public void goToForum(Account account, Forum forum) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerView, ForumFragment.newInstance(account, forum))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void goToForums(Account account) {
        currentUser = account;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerView, ForumsFragment.newInstance(account))
                .commit();
    }

    @Override
    public void goToForumsFromNewForum() {
        getSupportFragmentManager().popBackStack();
    }
}