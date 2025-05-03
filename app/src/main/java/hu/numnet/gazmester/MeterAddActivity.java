package hu.numnet.gazmester;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Locale;

public class MeterAddActivity extends AppCompatActivity {
    private TextInputEditText inputMeterNumber, inputAddress;
    private MaterialButton btnSave, btnBack;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference metersRef;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_add);
        inputMeterNumber = findViewById(R.id.inputMeterNumber);
        inputAddress = findViewById(R.id.inputAddress);
        btnSave = findViewById(R.id.btnSaveMeter);
        btnBack = findViewById(R.id.btnBackMeterAdd);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        metersRef = db.collection("users").document(user.getUid()).collection("meters");

        // Próbáljuk automatikusan kitölteni a címet GPS alapján
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        inputAddress.setText(addresses.get(0).getAddressLine(0));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        btnSave.setOnClickListener(v -> saveMeter());
        btnBack.setOnClickListener(v -> finish());
    }

    private void saveMeter() {
        String meterNumber = inputMeterNumber.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        if (TextUtils.isEmpty(meterNumber)) {
            Toast.makeText(this, "A gázóra szám kötelező!", Toast.LENGTH_SHORT).show();
            return;
        }
        MeterListActivity.Meter meter = new MeterListActivity.Meter(meterNumber, address, latitude, longitude);
        metersRef.add(meter).addOnSuccessListener(doc -> finish());
    }
}
