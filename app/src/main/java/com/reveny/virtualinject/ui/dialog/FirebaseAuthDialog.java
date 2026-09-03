package com.reveny.virtualinject.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.reveny.virtualinject.R;
import com.vcore.core.GmsNativeBridge;

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
    private Handler mHandler;
    private FirebaseAuthListener mListener;
    
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
        
        mHandler = new Handler(Looper.getMainLooper());
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
        if (mAccountName != null && !mAccountName.isEmpty()) {
            tvAccountInfo.setText("Account: " + mAccountName);
        } else {
            tvAccountInfo.setText("No account specified");
        }
    }
    
    private void startAuthentication() {
        btnAuthenticate.setEnabled(false);
        btnCancel.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        
        if (mListener != null) {
            mListener.onAuthStarted();
        }
        
        updateStatus("Authenticating with Firebase...");
        
        new Thread(() -> {
            try {
                // Initialize native bridge
                GmsNativeBridge bridge = GmsNativeBridge.getInstance();
                if (!bridge.isInitialized()) {
                    bridge.initialize(requireContext());
                }
                
                mHandler.post(() -> updateStatus("Generating Firebase token..."));
                Thread.sleep(1000);
                
                // Generate Firebase token
                String authToken = bridge.generateFirebaseToken(mAccountName);
                
                mHandler.post(() -> updateStatus("Generating ID token..."));
                Thread.sleep(500);
                
                // Generate ID token
                String clientId = "123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com";
                String idToken = bridge.generateIdToken(mAccountName, clientId);
                
                mHandler.post(() -> updateStatus("Storing tokens..."));
                Thread.sleep(500);
                
                // Store tokens
                bridge.storeFirebaseToken(mAccountName, authToken, "", idToken);
                
                mHandler.post(() -> updateStatus("Authentication successful!"));
                
                new Handler().postDelayed(() -> {
                    if (mListener != null) {
                        mListener.onAuthCompleted(mAccountName, authToken, idToken);
                    }
                    dismiss();
                }, 1000);
                
            } catch (Exception e) {
                mHandler.post(() -> {
                    updateStatus("Authentication failed: " + e.getMessage());
                    btnAuthenticate.setEnabled(true);
                    btnCancel.setEnabled(true);
                    
                    if (mListener != null) {
                        mListener.onAuthFailed(e.getMessage());
                    }
                });
            }
        }).start();
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
