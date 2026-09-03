package com.reveny.virtualinject.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.reveny.virtualinject.R;
import com.vcore.core.GmsInstaller;
import com.vcore.core.GmsNativeBridge;

public class GmsInstallDialog extends DialogFragment {
    private static final String TAG = "GmsInstallDialog";
    private static final String ARG_USER_ID = "user_id";
    
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Button btnInstall;
    private Button btnCancel;
    
    private int mUserId;
    private Handler mHandler;
    private GmsInstallListener mListener;
    
    public interface GmsInstallListener {
        void onInstallStarted();
        void onInstallProgress(int progress, String status);
        void onInstallCompleted(boolean success);
        void onInstallFailed(String error);
    }
    
    public static GmsInstallDialog newInstance(int userId) {
        GmsInstallDialog dialog = new GmsInstallDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.GmsDialogTheme);
        
        if (getArguments() != null) {
            mUserId = getArguments().getInt(ARG_USER_ID, 0);
        }
        
        mHandler = new Handler(Looper.getMainLooper());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_gms_install, container, false);
        
        initViews(view);
        setupListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        tvStatus = view.findViewById(R.id.tv_status);
        btnInstall = view.findViewById(R.id.btn_install);
        btnCancel = view.findViewById(R.id.btn_cancel);
    }
    
    private void setupListeners() {
        btnInstall.setOnClickListener(v -> startInstallation());
        btnCancel.setOnClickListener(v -> dismiss());
    }
    
    private void startInstallation() {
        btnInstall.setEnabled(false);
        btnCancel.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        
        if (mListener != null) {
            mListener.onInstallStarted();
        }
        
        updateStatus("Initializing GMS installation...");
        
        new Thread(() -> {
            try {
                // Initialize native bridge
                GmsNativeBridge bridge = GmsNativeBridge.getInstance();
                if (!bridge.isInitialized()) {
                    bridge.initialize(requireContext());
                }
                
                mHandler.post(() -> updateProgress(10, "Native bridge initialized"));
                Thread.sleep(500);
                
                // Install GMS bundle
                GmsInstaller installer = new GmsInstaller(requireContext(), mUserId);
                
                mHandler.post(() -> updateProgress(20, "Installing GMS core services..."));
                Thread.sleep(1000);
                
                // Simulate installation progress
                for (int i = 30; i <= 90; i += 10) {
                    final int progress = i;
                    mHandler.post(() -> updateProgress(progress, "Installing packages..."));
                    Thread.sleep(500);
                }
                
                // Perform actual installation
                installer.installCompleteGmsBundle();
                
                mHandler.post(() -> {
                    updateProgress(100, "Installation complete!");
                    
                    new Handler().postDelayed(() -> {
                        if (mListener != null) {
                            mListener.onInstallCompleted(true);
                        }
                        dismiss();
                    }, 1000);
                });
                
            } catch (Exception e) {
                mHandler.post(() -> {
                    updateStatus("Installation failed: " + e.getMessage());
                    btnInstall.setEnabled(true);
                    btnCancel.setEnabled(true);
                    
                    if (mListener != null) {
                        mListener.onInstallFailed(e.getMessage());
                    }
                });
            }
        }).start();
    }
    
    private void updateProgress(int progress, String status) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(progress);
        tvStatus.setText(status);
    }
    
    private void updateStatus(String status) {
        tvStatus.setText(status);
    }
    
    public void setGmsInstallListener(GmsInstallListener listener) {
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
