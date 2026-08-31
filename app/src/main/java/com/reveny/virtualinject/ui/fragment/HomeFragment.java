package com.reveny.virtualinject.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reveny.virtualinject.BuildConfig;
import com.reveny.virtualinject.R;
import com.reveny.virtualinject.databinding.DialogAboutBinding;
import com.reveny.virtualinject.databinding.FragmentHomeBinding;
import com.reveny.virtualinject.ui.dialog.BlurBehindDialogBuilder;
import com.reveny.virtualinject.util.Utility;
import com.reveny.virtualinject.util.chrome.LinkTransformationMethod;
import com.vcore.BlackBoxCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import rikka.material.app.LocaleDelegate;

public class HomeFragment extends BaseFragment {
    private static final String TAG = "VirtualInjectLog";

    private String selectedApp;
    private String selectedAppName;
    private String libraryPath;

    private FragmentHomeBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.about).setOnMenuItemClickListener(item -> {
            showAbout();
            return true;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 1 || resultCode != Activity.RESULT_OK) {
            return;
        }

        if (data == null || data.getData() == null) {
            Toast.makeText(getActivity(), "File selection failed", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = data.getData();

        String displayName = getFileName(fileUri);
        if (displayName == null || !displayName.endsWith(".so")) {
            Toast.makeText(getActivity(), "Please select a .so file", Toast.LENGTH_SHORT).show();
            return;
        }

        File dest = new File(requireContext().getCacheDir(), "libinject.so");

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
             OutputStream outputStream = new FileOutputStream(dest)) {

            if (inputStream == null) {
                Toast.makeText(getActivity(), "Failed to read file", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            libraryPath = dest.getAbsolutePath();
            Log.i(TAG, "Copied library file to: " + libraryPath);
            binding.libPath.setText(displayName);
            Toast.makeText(getActivity(), "Library loaded: " + displayName, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "Failed to copy library file", e);
            Toast.makeText(getActivity(), "Failed to copy library file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        name = cursor.getString(index);
                    }
                }
            }
        }
        if (name == null) {
            name = uri.getPath();
            if (name != null) {
                int cut = name.lastIndexOf('/');
                if (cut >= 0) {
                    name = name.substring(cut + 1);
                }
            }
        }
        return name;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupToolbar(binding.toolbar, null, R.string.app_name, R.menu.menu_home);
        binding.toolbar.setNavigationIcon(null);
        binding.toolbar.setOnClickListener(null);
        binding.appBar.setLiftable(true);
        binding.nestedScrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        setButtonsEnabled(false);
        binding.statusText.setVisibility(View.VISIBLE);
        binding.statusText.setText("Initializing services...");

        BlackBoxCore.get().setOnServicesReadyListener(() -> {
            if (binding == null) return;
            binding.statusText.setVisibility(View.GONE);
            setButtonsEnabled(true);
        });

        binding.fabAddApp.setOnClickListener(v -> showAppPicker());

        binding.libPathChoose.setEndIconOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("*/*");
            startActivityForResult(chooseFile, 1);
        });

        binding.installButton.setOnClickListener(v -> {
            if (selectedApp != null) {
                Log.i(TAG, "Installing: " + selectedApp);
                BlackBoxCore.get().installPackageAsUser(selectedApp, 0);

                boolean isInstalled = BlackBoxCore.get().isInstalled(selectedApp, 0);
                Log.i(TAG, "isInstalled: " + isInstalled);
                if (!isInstalled) {
                    Toast.makeText(requireContext(), "Failed to install", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), "Installed: " + selectedAppName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Tap + to select an app", Toast.LENGTH_SHORT).show();
            }
        });

        binding.launchButton.setOnClickListener(v -> {
            if (selectedApp != null && libraryPath != null) {
                boolean isInstalled = BlackBoxCore.get().isInstalled(selectedApp, 0);
                if (!isInstalled) {
                    Toast.makeText(requireContext(), "Please install the app first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "Launching: " + selectedApp);
                BlackBoxCore.get().launchApk(selectedApp, 0);
            } else {
                Toast.makeText(requireContext(), "Select an app and library first", Toast.LENGTH_SHORT).show();
            }
        });

        return binding.getRoot();
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.installButton.setEnabled(enabled);
        binding.launchButton.setEnabled(enabled);
        binding.fabAddApp.setEnabled(enabled);
    }

    private void showAppPicker() {
        if (getContext() == null) return;

        List<Utility.AppInfo> apps = Utility.getInstalledApps(requireContext());
        if (apps.isEmpty()) {
            Toast.makeText(requireContext(), "No apps found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] appNames = new String[apps.size()];
        String[] packageNames = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            appNames[i] = apps.get(i).appName + "\n" + apps.get(i).packageName;
            packageNames[i] = apps.get(i).packageName;
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select App to Clone")
            .setItems(appNames, (dialog, which) -> {
                selectedApp = packageNames[which];
                selectedAppName = apps.get(which).appName;
                binding.selectedAppLabel.setText(selectedAppName + "\n" + packageNames[which]);
                Log.i(TAG, "Selected: " + selectedApp);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    public static class AboutDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            DialogAboutBinding binding = DialogAboutBinding.inflate(getLayoutInflater(), null, false);
            setupAboutDialog(binding);
            return new BlurBehindDialogBuilder(requireContext()).setView(binding.getRoot()).create();
        }

        private void setupAboutDialog(DialogAboutBinding binding) {
            binding.designAboutTitle.setText(R.string.app_name);
            binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
            binding.designAboutInfo.setTransformationMethod(new LinkTransformationMethod(requireActivity()));
            binding.designAboutInfo.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://t.me/revenyy\">Telegram</a></b>",
                    "<b><a href=\"https://github.com/reveny/\">Reveny</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.designAboutVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        }
    }

    private void showAbout() {
        new AboutDialog().show(getChildFragmentManager(), "about");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
