package hu.numnet.gazmester;


import android.os.Bundle;

import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class ActionMenu extends AppCompatActivity {
    private static final String LOG_TAG = ActionMenu.class.getName();
    private static final String PREF_KEY = ActionMenu.class.getPackage().toString();

    private static final int SECRET_KEY = 99;

    FirebaseAuth mAuth;
    TextView debugTV;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_action_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        debugTV = findViewById(R.id.debugText);

        debugTV.setText(
                "User: " + mAuth.getCurrentUser().getEmail() + "\n" +
                "UID: " + mAuth.getCurrentUser().getUid() + "\n" +
                "Provider: " + mAuth.getCurrentUser().getProviderData().get(1).getProviderId() + "\n" +
                "Token: " + mAuth.getCurrentUser().getIdToken(false).toString()
        );

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void logout() {
        mAuth.signOut();

    }

}