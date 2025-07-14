package com.example.enerlink1;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class DeviceViewHolder extends RecyclerView.ViewHolder {
    CheckBox checkboxDevice;
    TextView tvDeviceName;

    public DeviceViewHolder(@NonNull View itemView) {
        super(itemView);
        checkboxDevice = itemView.findViewById(R.id.checkboxDevice);
        tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
    }
}

