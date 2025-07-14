package com.example.enerlink1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.bluetooth.BluetoothDevice;
import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder> {

    public interface OnConnectClickListener {
        void onConnectClick(BluetoothDevice device);
    }

    private Context context;
    private List<BluetoothDevice> deviceList;
    private OnConnectClickListener listener;

    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> deviceList, OnConnectClickListener listener) {
        this.context = context;
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);

        String deviceName;
        String deviceAddress;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            deviceName = device.getName();
            deviceAddress = device.getAddress();
        } else {
            deviceAddress = null;
            deviceName = null;
        }

        holder.tvDeviceName.setText(deviceName != null ? deviceName : (deviceAddress != null ? deviceAddress : "Unknown Device"));

        holder.btnConnect.setOnClickListener(v -> {
            // Assign inside lambda to avoid "effectively final" error
            String name = deviceName;
            String address = deviceAddress;

            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("device_name", name != null ? name : "Unknown Device");
            intent.putExtra("device_address", address != null ? address : "");
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        ImageButton btnConnect;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}
