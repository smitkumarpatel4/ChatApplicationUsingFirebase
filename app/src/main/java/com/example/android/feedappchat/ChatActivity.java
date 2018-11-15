package com.example.android.feedappchat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.android.feedappchat.Adapter.MessageAdapter;
import com.example.android.feedappchat.Model.Message;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;

    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    //firebase database varieable
    private FirebaseDatabase mFbDatabase;
    private DatabaseReference mMessageDbReference;
    private ChildEventListener mChildEventListener;



    private FirebaseAuth mFbAuth;
    private FirebaseAuth.AuthStateListener mAuthSateListner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<Message> MessagesList = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.chat_item, MessagesList);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        //Initializing Firebase Function
        mFbDatabase = FirebaseDatabase.getInstance();
        mFbAuth = FirebaseAuth.getInstance();


        mMessageDbReference = mFbDatabase.getReference().child("messages");

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click
                //get a message from Message Edit text
                Message message = new Message(mMessageEditText.getText().toString() , mUsername, null);

                // send a message to the database reference
                mMessageDbReference.push().setValue(message);
                // Clear input box
                mMessageEditText.setText("");
            }
        });


        //Authentication Listner
        mAuthSateListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // Check the whether user is sign in or not
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if ( user  != null){
                    // user is signed in

                    onSignedInInitialize(user.getDisplayName());

                }
                else{
                    //user is signed out
                    onSignedOutInitialize();

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()
//                                            new AuthUI.IdpConfig.FacebookBuilder().build(),
//                                            new AuthUI.IdpConfig.TwitterBuilder().build(),
//                                            new AuthUI.IdpConfig.GitHubBuilder().build(),
//                                            new AuthUI.IdpConfig.PhoneBuilder().build()
                                            )
                                    )
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.sign_out_menu:
                return true;
            default:
                    return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN){
            if (requestCode ==RESULT_OK){
                Toast.makeText(this,"Signed in!",Toast.LENGTH_LONG).show();
            }
            else if (requestCode == RESULT_CANCELED){
                Toast.makeText(this,"Signed in Canceled!",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mAuthSateListner != null) {
            mFbAuth.addAuthStateListener(mAuthSateListner);
        }
        detachDatabaseReadListner();
        mMessageAdapter.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFbAuth.removeAuthStateListener(mAuthSateListner);
    }
    private void onSignedInInitialize(String username){
        mUsername = username;
        attachDatabaseReadListner();
    }
    private void onSignedOutInitialize(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListner();
    }
    private void attachDatabaseReadListner(){
        //Child event Listner for geting message
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    Message message = dataSnapshot.getValue(Message.class);
                    mMessageAdapter.add(message);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            };
        }

        mMessageDbReference.addChildEventListener(mChildEventListener);

    }

    private void detachDatabaseReadListner(){

        if (mChildEventListener !=  null) {
            mMessageDbReference.removeEventListener(mChildEventListener);
        }
    }
}