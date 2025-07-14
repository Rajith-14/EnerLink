package com.example.enerlink1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScanActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_ENABLE_BT = 101;

    private RecyclerView rvNearbyDevices, rvPairedDevices;
    private BluetoothDeviceAdapter pairedAdapter, nearbyAdapter;
    private List<BluetoothDevice> pairedList = new ArrayList<>();
    private List<BluetoothDevice> nearbyList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ImageButton btnCreateGroup;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !nearbyList.contains(device)) {
                    nearbyList.add(device);
                    nearbyAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(ScanActivity.this, "Discovery Finished", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        rvNearbyDevices = findViewById(R.id.rvNearbyDevices);
        rvPairedDevices = findViewById(R.id.rvPairedDevices);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        rvNearbyDevices.setLayoutManager(new LinearLayoutManager(this));
        rvPairedDevices.setLayoutManager(new LinearLayoutManager(this));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        if (!hasPermissions()) {
            requestPermissions();
        } else {
            initBluetooth();
        }

        btnCreateGroup.setOnClickListener(v -> showGroupCreationDialog());
    }

    private void showGroupCreationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_group_create, null);
        RecyclerView rvGroupDevices = dialogView.findViewById(R.id.rvGroupDevices);
        Button btnSubmitGroup = dialogView.findViewById(R.id.btnSubmitGroup);

        GroupDeviceAdapter adapter = new GroupDeviceAdapter(pairedList);
        rvGroupDevices.setLayoutManager(new LinearLayoutManager(this));
        rvGroupDevices.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnSubmitGroup.setOnClickListener(submit -> {
            Set<BluetoothDevice> selected = adapter.getSelectedDevices();
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select at least one device", Toast.LENGTH_SHORT).show();
            } else {
                ArrayList<String> addresses = new ArrayList<>();
                for (BluetoothDevice device : selected) {
                    addresses.add(device.getAddress());
                }
                Intent intent = new Intent(this, GroupChatActivity.class);
                intent.putStringArrayListExtra("group_device_addresses", addresses);
                startActivity(intent);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void initBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // Request necessary Bluetooth permissions
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, REQUEST_PERMISSIONS);
                    return; // Exit and wait for user response
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, REQUEST_PERMISSIONS);
                    return;
                }
            }

            // Permissions granted, proceed
            loadPairedDevices();
            startDiscovery();
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void loadPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedList.clear();
        if (pairedDevices != null) pairedList.addAll(pairedDevices);

        pairedAdapter = new BluetoothDeviceAdapter(this, pairedList, device -> {
            Intent intent = new Intent(ScanActivity.this, ChatActivity.class);
            intent.putExtra("device_name", device.getName());
            intent.putExtra("device_address", device.getAddress());
            startActivity(intent);
        });
        rvPairedDevices.setAdapter(pairedAdapter);
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            return;

        nearbyList.clear();
        bluetoothAdapter.cancelDiscovery();
        bluetoothAdapter.startDiscovery();

        nearbyAdapter = new BluetoothDeviceAdapter(this, nearbyList, device -> {
            if (pairedList.contains(device)) {
                Intent intent = new Intent(ScanActivity.this, ChatActivity.class);
                intent.putExtra("device_name", device.getName());
                intent.putExtra("device_address", device.getAddress());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Device is not paired. Pair it first.", Toast.LENGTH_SHORT).show();
            }
        });
        rvNearbyDevices.setAdapter(nearbyAdapter);
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            initBluetooth();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(discoveryReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && hasPermissions()) {
            initBluetooth();
        } else {
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show();
        }
    }
}
