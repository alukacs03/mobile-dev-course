package hu.numnet.gazmester;

import android.Manifest;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import android.os.Bundle;

public class MeterEditActivity extends AppCompatActivity {
    private TextInputEditText inputMeterNumber, inputAddress;
    private MaterialButton btnSave, btnDelete, btnBack;
    private ImageButton btnMapPreview;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference metersRef;
    private String meterId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_edit);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        inputMeterNumber = findViewById(R.id.inputMeterNumber);
        inputAddress = findViewById(R.id.inputAddress);
        btnSave = findViewById(R.id.btnSaveMeter);
        btnDelete = findViewById(R.id.btnDeleteMeter);
        btnMapPreview = findViewById(R.id.btnMapPreview);
        btnBack = findViewById(R.id.btnBackMeter);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        metersRef = db.collection("users").document(user.getUid()).collection("meters");
        meterId = getIntent().getStringExtra("METER_ID");
        if (meterId != null) {
            btnDelete.setVisibility(View.VISIBLE);
            metersRef.document(meterId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    inputMeterNumber.setText(doc.getString("meterNumber"));
                    inputAddress.setText(doc.getString("address"));
                }
            });
        } else {
            btnDelete.setVisibility(View.GONE);
        }
        btnSave.setOnClickListener(v -> saveMeter());
        btnDelete.setOnClickListener(v -> deleteMeter());
        btnMapPreview.setOnClickListener(v -> openMap());
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void openMap() {
        String address = inputAddress.getText().toString();
        if (!TextUtils.isEmpty(address)) {
            String uri = "geo:0,0?q=" + android.net.Uri.encode(address);
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Nem található térkép alkalmazás!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Nincs elérhető cím!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMeter() {
        String meterNumber = inputMeterNumber.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        if (TextUtils.isEmpty(meterNumber)) {
            Toast.makeText(this, "A gázóra szám kötelező!", Toast.LENGTH_SHORT).show();
            return;
        }
        MeterListActivity.Meter meter = new MeterListActivity.Meter(meterNumber, address, 0.0, 0.0);
        if (meterId == null) {
            metersRef.add(meter).addOnSuccessListener(doc -> finish());
        } else {
            metersRef.document(meterId).set(meter).addOnSuccessListener(doc -> finish());
        }
    }

    private void deleteMeter() {
        if (meterId != null) {
            metersRef.document(meterId).delete().addOnSuccessListener(doc -> finish());
        }
    }
}
