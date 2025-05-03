package hu.numnet.gazmester;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditDataActivity extends AppCompatActivity {
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private Button btnSaveData;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_data);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnSaveData = findViewById(R.id.btnSaveData);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            inputEmail.setText(user.getEmail());
        }
        btnSaveData.setOnClickListener(v -> saveData());
        findViewById(R.id.btnBackEditData).setOnClickListener(v -> finish());
    }

    private void saveData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            return;
        }
        String newEmail = inputEmail.getText().toString().trim();
        String newPassword = inputPassword.getText().toString().trim();
        if (!TextUtils.isEmpty(newEmail) && !newEmail.equals(user.getEmail())) {
            user.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Email sikeresen frissítve!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Email frissítés sikertelen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        if (!TextUtils.isEmpty(newPassword)) {
            user.updatePassword(newPassword)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Jelszó sikeresen frissítve!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Jelszó frissítés sikertelen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        if ((TextUtils.isEmpty(newPassword)) && (TextUtils.isEmpty(newEmail) || newEmail.equals(user.getEmail()))) {
            Toast.makeText(this, "Nincs változás!", Toast.LENGTH_SHORT).show();
        }
    }
}
