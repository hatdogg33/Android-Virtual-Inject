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

public class GoogleSignInDialog extends DialogFragment {
    private static final String TAG = "GoogleSignInDialog";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_ACCOUNT_NAME = "account_name";
    
    private ImageView ivGoogleLogo;
    private TextView tvAccountName;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnSignIn;
    private Button btnCancel;
    
    private int mUserId;
    private String mAccountName;
    private Handler mHandler;
    private GoogleSignInListener mListener;
    
    public interface GoogleSignInListener {
        void onSignInStarted();
        void onSignInProgress(String status);
        void onSignInCompleted(String accountName, String authToken);
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
        
        mHandler = new Handler(Looper.getMainLooper());
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
        
        updateStatus("Signing in to Google...");
        
        new Thread(() -> {
            try {
                // Initialize native bridge
                GmsNativeBridge bridge = GmsNativeBridge.getInstance();
                if (!bridge.isInitialized()) {
                    bridge.initialize(requireContext());
                }
                
                mHandler.post(() -> updateStatus("Generating authentication token..."));
                Thread.sleep(1000);
                
                // Generate Google auth token
                String authToken = bridge.generateGoogleAuthToken(mAccountName);
                
                mHandler.post(() -> updateStatus("Creating account..."));
                Thread.sleep(500);
                
                // Generate device info if not exists
                GmsNativeBridge.DeviceInfo deviceInfo = bridge.loadDeviceInfo();
                if (deviceInfo == null) {
                    deviceInfo = new GmsNativeBridge.DeviceInfo();
                    deviceInfo.androidId = bridge.generateSecureToken(16);
                    deviceInfo.deviceId = bridge.generateSecureToken(32);
                    deviceInfo.gaiaId = String.valueOf(mAccountName.hashCode());
                    deviceInfo.model = android.os.Build.MODEL;
                    deviceInfo.manufacturer = android.os.Build.MANUFACTURER;
                    deviceInfo.brand = android.os.Build.BRAND;
                    bridge.storeDeviceInfo(deviceInfo);
                }
                
                mHandler.post(() -> updateStatus("Sign in successful!"));
                
                new Handler().postDelayed(() -> {
                    if (mListener != null) {
                        mListener.onSignInCompleted(mAccountName, authToken);
                    }
                    dismiss();
                }, 1000);
                
            } catch (Exception e) {
                mHandler.post(() -> {
                    updateStatus("Sign in failed: " + e.getMessage());
                    btnSignIn.setEnabled(true);
                    btnCancel.setEnabled(true);
                    
                    if (mListener != null) {
                        mListener.onSignInFailed(e.getMessage());
                    }
                });
            }
        }).start();
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
