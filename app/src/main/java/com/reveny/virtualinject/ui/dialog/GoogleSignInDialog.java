package com.reveny.virtualinject.ui.dialog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.reveny.virtualinject.R;

public class GoogleSignInDialog extends DialogFragment {
    private static final String TAG = "GoogleSignInDialog";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_ACCOUNT_NAME = "account_name";
    private static final int RC_SIGN_IN = 9001;

    private ImageView ivGoogleLogo;
    private TextView tvAccountName;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnSignIn;
    private Button btnCancel;

    private int mUserId;
    private String mAccountName;
    private GoogleSignInListener mListener;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    public interface GoogleSignInListener {
        void onSignInStarted();
        void onSignInProgress(String status);
        void onSignInCompleted(String accountName, String authToken, String idToken);
        void onSignInFailed(String error);
        void onSignInCancelled();
    }

    public static GoogleSignInDialog newInstance(int userId, String accountName) {
        GoogleSignInDialog dialog = new GoogleSignInDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.GoogleSignInDialogTheme);

        if (getArguments() != null) {
            mUserId = getArguments().getInt(ARG_USER_ID, 0);
            mAccountName = getArguments().getString(ARG_ACCOUNT_NAME, "");
        }

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("884035582605-oud2mpv5if142nerikbcchh17pjhi0ak.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_google_signin, container, false);

        initViews(view);
        setupListeners();
        updateAccountInfo();

        return view;
    }

    private void initViews(View view) {
        ivGoogleLogo = view.findViewById(R.id.iv_google_logo);
        tvAccountName = view.findViewById(R.id.tv_account_name);
        tvStatus = view.findViewById(R.id.tv_status);
        progressBar = view.findViewById(R.id.progress_bar);
        btnSignIn = view.findViewById(R.id.btn_sign_in);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    private void setupListeners() {
        btnSignIn.setOnClickListener(v -> startSignIn());
        btnCancel.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSignInCancelled();
            }
            dismiss();
        });
    }

    private void updateAccountInfo() {
        if (mAccountName != null && !mAccountName.isEmpty()) {
            tvAccountName.setText(mAccountName);
        } else {
            tvAccountName.setText("No account selected");
        }
    }

    private void startSignIn() {
        btnSignIn.setEnabled(false);
        btnCancel.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        if (mListener != null) {
            mListener.onSignInStarted();
        }

        updateStatus("Opening Google sign-in...");
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account == null) {
                    handleFailure("Sign in cancelled");
                    return;
                }
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                handleFailure("Google sign-in failed: " + e.getStatusCode());
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        updateStatus("Authenticating with Firebase...");
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String accountEmail = account.getEmail() != null ? account.getEmail() : mAccountName;
                String idToken = account.getIdToken();
                String serverAuthCode = account.getServerAuthCode();
                updateStatus("Sign in successful!");
                if (mListener != null) {
                    mListener.onSignInCompleted(accountEmail, serverAuthCode != null ? serverAuthCode : idToken, idToken);
                }
                dismiss();
            } else {
                handleFailure("Firebase auth failed: " +
                        (task.getException() != null ? task.getException().getMessage() : "unknown"));
            }
        });
    }

    private void handleFailure(String error) {
        progressBar.setVisibility(View.GONE);
        btnSignIn.setEnabled(true);
        btnCancel.setEnabled(true);
        updateStatus(error);
        if (mListener != null) {
            mListener.onSignInFailed(error);
        }
    }

    private void updateStatus(String status) {
        tvStatus.setText(status);
    }

    public void setGoogleSignInListener(GoogleSignInListener listener) {
        mListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
