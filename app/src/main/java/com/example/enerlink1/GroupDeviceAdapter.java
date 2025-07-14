package com.example.enerlink1;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupDeviceAdapter extends RecyclerView.Adapter<GroupDeviceAdapter.DeviceViewHolder> {

    private List<BluetoothDevice> deviceList;
    private Set<BluetoothDevice> selectedDevices = new HashSet<>();

    public GroupDeviceAdapter(List<BluetoothDevice> devices) {
        this.deviceList = devices;
    }

    public Set<BluetoothDevice> getSelectedDevices() {
        return selectedDevices;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_devices, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);

        // Use the correct context here
        if (ActivityCompat.checkSelfPermission(holder.itemView.getContext(), Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String name = device.getName() != null ? device.getName() : "Unnamed";

        holder.checkBox.setText(name);
        holder.checkBox.setChecked(selectedDevices.contains(device));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedDevices.add(device);
            } else {
                selectedDevices.remove(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        DeviceViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxDevice);
        }
    }
}
