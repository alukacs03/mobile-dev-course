package hu.numnet.gazmester;


import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class ActionMenu extends AppCompatActivity {
    private static final String LOG_TAG = ActionMenu.class.getName();
    private static final String PREF_KEY = ActionMenu.class.getPackage().toString();

    private static final int SECRET_KEY = 99;

    CardView newReportCard;
    CardView viewReportsCard;
    CardView editDataCard;
    CardView manageMetersCard;
    CardView logoutCard;

    FirebaseAuth mAuth;

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
        newReportCard = findViewById(R.id.newReportCard);
        viewReportsCard = findViewById(R.id.viewReportsCard);
        editDataCard = findViewById(R.id.editDataCard);
        manageMetersCard = findViewById(R.id.manageMetersCard);
        logoutCard = findViewById(R.id.logoutCard);


        Animation slideInLeft1 = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation slideInLeft2 = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation slideInLeft3 = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation slideInLeft4 = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation slideInLeft5 = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);

        slideInLeft2.setStartOffset(200);
        slideInLeft3.setStartOffset(400);
        slideInLeft4.setStartOffset(600);
        slideInLeft5.setStartOffset(800);

        newReportCard.startAnimation(slideInLeft1);
        viewReportsCard.startAnimation(slideInLeft2);
        editDataCard.startAnimation(slideInLeft3);
        manageMetersCard.startAnimation(slideInLeft4);
        logoutCard.startAnimation(slideInLeft5);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void logout() {
        mAuth.signOut();

    }

    public void newReport(View view) {
        Intent intent = new Intent(this, NewReadingActivity.class);
        startActivity(intent);
    }

    public void viewReports(View view) {
        Intent intent = new Intent(this, ReadingListActivity.class);
        startActivity(intent);
    }

    public void editData(View view) {
        Intent intent = new Intent(this, EditDataActivity.class);
        startActivity(intent);
    }

    public void logout(View view) {
        mAuth.signOut();
        Toast.makeText(this, "Sikeresen kijelentkezett", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void manageMeters(View view) {
        Intent intent = new Intent(this, MeterListActivity.class);
        startActivity(intent);
    }
}