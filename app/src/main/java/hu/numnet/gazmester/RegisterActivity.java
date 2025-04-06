package hu.numnet.gazmester;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = RegisterActivity.class.getName();
    private static final String PREF_KEY = RegisterActivity.class.getPackage().toString();


    EditText emailET;
    EditText passwordET;
    EditText passwordConfirmET;
    EditText nameET;
    EditText addressET;
    EditText phoneET;
    Spinner consumerTypeSpinner;

    private SharedPreferences prefs;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        emailET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        passwordConfirmET = findViewById(R.id.passwordAgainET);
        nameET = findViewById(R.id.nameET);
        addressET = findViewById(R.id.addressET);
        phoneET = findViewById(R.id.phoneET);
        prefs = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        consumerTypeSpinner = findViewById(R.id.consumerTypeSpinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.consumerType, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        consumerTypeSpinner.setAdapter(adapter);
        consumerTypeSpinner.setSelection(0);

        // for testing purposes
        mAuth.signOut();

        // Check if the user is already logged in
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(this, ActionMenu.class);
            intent.putExtra("SECRET_KEY", 99);
            startActivity(intent);
            finish();
        }

        String email = prefs.getString("email", "");
        String password = prefs.getString("password", "");
        emailET.setText(email);
        passwordET.setText(password);
        passwordConfirmET.setText(password);

    }

    public void register(View view) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        String passwordConfirm = passwordConfirmET.getText().toString();

        if (!password.equals(passwordConfirm)) {
            passwordConfirmET.setError("Passwords do not match");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(RegisterActivity.this, ActionMenu.class);
                        intent.putExtra("SECRET_KEY", 99);
                        startActivity(intent);
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        passwordET.setError("Authentication failed.");
                        Toast.makeText(this, passwordET.getError(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void login(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("SECRET_KEY", 99);
        startActivity(intent);
    }
}