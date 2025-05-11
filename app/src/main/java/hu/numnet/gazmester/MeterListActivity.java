package hu.numnet.gazmester;

import android.app.AlertDialog;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MeterListActivity extends AppCompatActivity {
    private ListView meterListView;
    private List<Meter> meterObjList = new ArrayList<>();
    private List<String> meterDocIds = new ArrayList<>();
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference metersRef;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;
    private MeterAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_list);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        meterListView = findViewById(R.id.meterListView);
        meterObjList = new ArrayList<>();
        adapter = new MeterAdapter();
        meterListView.setAdapter(adapter);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        metersRef = db.collection("users").document(user.getUid()).collection("meters");
        getLastLocation();
        findViewById(R.id.btnAddMeter).setOnClickListener(v -> {
            startActivity(new Intent(this, MeterAddActivity.class));
        });
        findViewById(R.id.btnBackMeterList).setOnClickListener(v -> finish());
        meterListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteMeterDialog(meterObjList.get(position).getMeterNumber());
            return true;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeters();
    }

    private void getLastLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                lastLatitude = location.getLatitude();
                lastLongitude = location.getLongitude();
            }
        }
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    private void loadMeters() {
        meterObjList.clear();
        meterDocIds.clear();
        metersRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Meter meter = doc.toObject(Meter.class);
                meterObjList.add(meter);
                meterDocIds.add(doc.getId());
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showDeleteMeterDialog(String meterNumber) {
        new AlertDialog.Builder(this)
            .setTitle("Gázóra törlése")
            .setMessage("Biztosan törlöd ezt a gázórát?\n" + meterNumber)
            .setPositiveButton("Törlés", (dialog, which) -> deleteMeter(meterNumber))
            .setNegativeButton("Mégse", null)
            .show();
    }

    private void deleteMeter(String meterNumber) {
        metersRef.whereEqualTo("meterNumber", meterNumber).get().addOnSuccessListener(query -> {
            for (QueryDocumentSnapshot doc : query) {
                doc.getReference().delete();
            }
            loadMeters();
        });
    }

    private class MeterAdapter extends BaseAdapter {
        @Override
        public int getCount() { return meterObjList.size(); }
        @Override
        public Object getItem(int position) { return meterObjList.get(position); }
        @Override
        public long getItemId(int position) { return position; }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(MeterListActivity.this).inflate(R.layout.meter_list_item, parent, false);
            }
            Meter meter = meterObjList.get(position);
            TextView meterNumber = view.findViewById(R.id.textMeterNumber);
            TextView meterAddress = view.findViewById(R.id.textMeterAddress);
            ImageButton btnMap = view.findViewById(R.id.btnOpenMap);
            ImageButton btnEdit = view.findViewById(R.id.btnEditMeter);
            meterNumber.setText(meter.getMeterNumber());
            meterAddress.setText(meter.getAddress() != null && !meter.getAddress().isEmpty() ? meter.getAddress() : "(nincs cím)");
            btnMap.setOnClickListener(v -> {
                String uri;
                if (meter.getLatitude() != 0.0 && meter.getLongitude() != 0.0) {
                    uri = "geo:" + meter.getLatitude() + "," + meter.getLongitude() + "?q=" + meter.getLatitude() + "," + meter.getLongitude();
                } else if (meter.getAddress() != null && !meter.getAddress().isEmpty()) {
                    uri = "geo:0,0?q=" + Uri.encode(meter.getAddress());
                } else {
                    Toast.makeText(MeterListActivity.this, "Nincs elérhető cím vagy koordináta!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MeterListActivity.this, "Nem található térkép alkalmazás!", Toast.LENGTH_SHORT).show();
                }
            });
            btnEdit.setOnClickListener(v -> {
                String meterId = null;
                if (position < meterDocIds.size()) {
                    meterId = meterDocIds.get(position);
                }
                Intent intent = new Intent(MeterListActivity.this, MeterEditActivity.class);
                if (meterId != null) intent.putExtra("METER_ID", meterId);
                startActivity(intent);
            });
            return view;
        }
    }

    public static class Meter {
        private String meterNumber;
        private String address;
        private double latitude;
        private double longitude;
        public Meter() {}
        public Meter(String meterNumber, String address, double latitude, double longitude) {
            this.meterNumber = meterNumber;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        public String getMeterNumber() { return meterNumber; }
        public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
    }
}
