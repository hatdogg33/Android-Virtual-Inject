package com.reveny.virtualinject.ui.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reveny.virtualinject.BuildConfig;
import com.reveny.virtualinject.R;
import com.reveny.virtualinject.databinding.DialogAboutBinding;
import com.reveny.virtualinject.databinding.FragmentHomeBinding;
import com.reveny.virtualinject.ui.adapter.ClonedAppsAdapter;
import com.reveny.virtualinject.ui.dialog.BlurBehindDialogBuilder;
import com.reveny.virtualinject.util.Utility;
import com.reveny.virtualinject.util.chrome.LinkTransformationMethod;
import com.vcore.BlackBoxCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import rikka.material.app.LocaleDelegate;

public class HomeFragment extends BaseFragment {
    private static final String TAG = "VirtualInjectLog";
    private static final int PICK_SO_FILE = 1;

    private FragmentHomeBinding binding;
    private ClonedAppsAdapter adapter;
    private String pendingInjectPackage;
    private String pendingInjectAppName;

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

        if (requestCode != PICK_SO_FILE || resultCode != Activity.RESULT_OK) {
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

            String libraryPath = dest.getAbsolutePath();
            Log.i(TAG, "Library loaded: " + libraryPath);

            if (pendingInjectPackage != null) {
                Toast.makeText(getActivity(), "Injecting " + pendingInjectAppName + "...", Toast.LENGTH_SHORT).show();
                BlackBoxCore.get().launchApk(pendingInjectPackage, 0);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to copy library file", e);
            Toast.makeText(getActivity(), "Failed to load library", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
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

        binding.statusText.setVisibility(View.VISIBLE);
        binding.statusText.setText("Initializing services...");

        binding.clonedAppsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ClonedAppsAdapter();
        binding.clonedAppsList.setAdapter(adapter);

        adapter.setOnAppClickListener(new ClonedAppsAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(Utility.AppInfo app) {
                showAppOptionsDialog(app);
            }

            @Override
            public void onInjectClick(Utility.AppInfo app) {
                startInject(app);
            }
        });

        binding.fabAddApp.setOnClickListener(v -> showCloneDialog());

        BlackBoxCore.get().setOnServicesReadyListener(() -> {
            if (binding == null) return;
            binding.statusText.setVisibility(View.GONE);
            refreshClonedApps();
        });

        return binding.getRoot();
    }

    private void refreshClonedApps() {
        if (!BlackBoxCore.get().isServicesReady()) return;

        try {
            List<Utility.AppInfo> clonedApps = new ArrayList<>();
            var packages = BlackBoxCore.get().getInstalledPackages(0, 0);
            if (packages != null) {
                android.content.pm.PackageManager pm = requireContext().getPackageManager();
                for (var pkg : packages) {
                    String appName = pkg.applicationInfo != null
                        ? pm.getApplicationLabel(pkg.applicationInfo).toString()
                        : pkg.packageName;
                    clonedApps.add(new Utility.AppInfo(appName, pkg.packageName));
                }
            }

            adapter.setApps(clonedApps);
            binding.clonedAppsList.setVisibility(clonedApps.isEmpty() ? View.GONE : View.VISIBLE);
            binding.emptyState.setVisibility(clonedApps.isEmpty() ? View.VISIBLE : View.GONE);

        } catch (Exception e) {
            Log.e(TAG, "Failed to load cloned apps", e);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.emptyState.setText("Failed to load apps");
        }
    }

    private void showCloneDialog() {
        if (!BlackBoxCore.get().isServicesReady()) {
            Toast.makeText(requireContext(), "Services not ready yet, please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
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
                    cloneApp(apps.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show clone dialog", e);
            Toast.makeText(requireContext(), "Failed to load apps", Toast.LENGTH_SHORT).show();
        }
    }

    private void cloneApp(Utility.AppInfo app) {
        Log.i(TAG, "Cloning: " + app.packageName);
        BlackBoxCore.get().installPackageAsUser(app.packageName, 0);

        boolean isInstalled = BlackBoxCore.get().isInstalled(app.packageName, 0);
        if (isInstalled) {
            Toast.makeText(requireContext(), "Cloned: " + app.appName, Toast.LENGTH_SHORT).show();
            refreshClonedApps();
        } else {
            Toast.makeText(requireContext(), "Failed to clone " + app.appName, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppOptionsDialog(Utility.AppInfo app) {
        String[] options = {"Inject .so", "Launch", "Uninstall"};

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(app.appName)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        startInject(app);
                        break;
                    case 1:
                        BlackBoxCore.get().launchApk(app.packageName, 0);
                        break;
                    case 2:
                        BlackBoxCore.get().uninstallPackage(app.packageName);
                        Toast.makeText(requireContext(), "Uninstalled: " + app.appName, Toast.LENGTH_SHORT).show();
                        refreshClonedApps();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void startInject(Utility.AppInfo app) {
        pendingInjectPackage = app.packageName;
        pendingInjectAppName = app.appName;

        Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("*/*");
        startActivityForResult(chooseFile, PICK_SO_FILE);
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
