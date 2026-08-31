package com.reveny.virtualinject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.reveny.virtualinject.R;
import com.reveny.virtualinject.util.Utility;

import java.util.ArrayList;
import java.util.List;

public class ClonedAppsAdapter extends RecyclerView.Adapter<ClonedAppsAdapter.ViewHolder> {

    private final List<Utility.AppInfo> apps = new ArrayList<>();
    private OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(Utility.AppInfo app);
        void onInjectClick(Utility.AppInfo app);
    }

    public void setOnAppClickListener(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void setApps(List<Utility.AppInfo> newApps) {
        apps.clear();
        apps.addAll(newApps);
        notifyDataSetChanged();
    }

    public void addApp(Utility.AppInfo app) {
        apps.add(0, app);
        notifyItemInserted(0);
    }

    public void removeApp(String packageName) {
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).packageName.equals(packageName)) {
                apps.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public int findPositionByPackage(String packageName) {
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).packageName.equals(packageName)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cloned_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Utility.AppInfo app = apps.get(position);
        holder.appName.setText(app.appName);
        holder.packageName.setText(app.packageName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppClick(app);
        });

        holder.btnInject.setOnClickListener(v -> {
            if (listener != null) listener.onInjectClick(app);
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appName;
        TextView packageName;
        MaterialButton btnInject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.app_name);
            packageName = itemView.findViewById(R.id.package_name);
            btnInject = itemView.findViewById(R.id.btn_inject);
        }
    }
}
