package com.reveny.virtualinject.ui.dialog;

import android.app.Dialog;
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

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.reveny.virtualinject.R;

public class FirebaseAuthDialog extends DialogFragment {
    private static final String TAG = "FirebaseAuthDialog";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_ACCOUNT_NAME = "account_name";

    private ImageView ivFirebaseLogo;
    private TextView tvAccountInfo;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnAuthenticate;
    private Button btnCancel;

    private int mUserId;
    private String mAccountName;
    private FirebaseAuthListener mListener;
    private FirebaseAuth mAuth;

    public interface FirebaseAuthListener {
        void onAuthStarted();
        void onAuthProgress(String status);
        void onAuthCompleted(String accountName, String authToken, String idToken);
        void onAuthFailed(String error);
        void onAuthCancelled();
    }

    public static FirebaseAuthDialog newInstance(int userId, String accountName) {
        FirebaseAuthDialog dialog = new FirebaseAuthDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FirebaseAuthDialogTheme);

        if (getArguments() != null) {
            mUserId = getArguments().getInt(ARG_USER_ID, 0);
            mAccountName = getArguments().getString(ARG_ACCOUNT_NAME, "");
        }

        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_firebase_auth, container, false);

        initViews(view);
        setupListeners();
        updateAccountInfo();

        return view;
    }

    private void initViews(View view) {
        ivFirebaseLogo = view.findViewById(R.id.iv_firebase_logo);
        tvAccountInfo = view.findViewById(R.id.tv_account_info);
        tvStatus = view.findViewById(R.id.tv_status);
        progressBar = view.findViewById(R.id.progress_bar);
        btnAuthenticate = view.findViewById(R.id.btn_authenticate);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }

    private void setupListeners() {
        btnAuthenticate.setOnClickListener(v -> startAuthentication());
        btnCancel.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onAuthCancelled();
            }
            dismiss();
        });
    }

    private void updateAccountInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            tvAccountInfo.setText("Signed in as: " + (displayName != null ? displayName : (email != null ? email : currentUser.getUid())));
        } else if (mAccountName != null && !mAccountName.isEmpty()) {
            tvAccountInfo.setText("Account: " + mAccountName);
        } else {
            tvAccountInfo.setText("Not signed in - sign in with Google first");
        }
    }

    private void startAuthentication() {
        btnAuthenticate.setEnabled(false);
        btnCancel.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        if (mListener != null) {
            mListener.onAuthStarted();
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            handleFailure("Not signed in. Use Google Sign-In first.");
            return;
        }

        updateStatus("Requesting Firebase ID token...");
        if (mListener != null) {
            mListener.onAuthProgress("Requesting Firebase ID token...");
        }

        Task<GetTokenResult> tokenTask = currentUser.getIdToken(true);
        tokenTask.addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String idToken = task.getResult().getToken();
                String uid = currentUser.getUid();
                updateStatus("Authentication successful!");
                if (mListener != null) {
                    mListener.onAuthCompleted(uid, idToken, idToken);
                }
                dismiss();
            } else {
                handleFailure("Failed to get ID token: " +
                        (task.getException() != null ? task.getException().getMessage() : "unknown"));
            }
        });
    }

    private void handleFailure(String error) {
        progressBar.setVisibility(View.GONE);
        btnAuthenticate.setEnabled(true);
        btnCancel.setEnabled(true);
        updateStatus(error);
        if (mListener != null) {
            mListener.onAuthFailed(error);
        }
    }

    private void updateStatus(String status) {
        tvStatus.setText(status);
    }

    public void setFirebaseAuthListener(FirebaseAuthListener listener) {
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
