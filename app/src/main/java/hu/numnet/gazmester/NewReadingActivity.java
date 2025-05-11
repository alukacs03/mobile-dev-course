package hu.numnet.gazmester;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.PersistableBundle;
import androidx.core.app.NotificationCompat;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.os.SystemClock;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import java.util.List;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.widget.Spinner;
import android.widget.ArrayAdapter;

import java.io.OutputStream;
import java.util.Calendar;

import com.google.android.material.appbar.MaterialToolbar;

public class NewReadingActivity extends AppCompatActivity {

    private static final String LOG_TAG = NewReadingActivity.class.getName();
    private static final String PREF_KEY = NewReadingActivity.class.getPackage().toString();
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    EditText inputDatum;
    Button btnMent;
    EditText inputOraAllas;
    Spinner spinnerMeters;
    ArrayAdapter<String> metersAdapter;
    List<String> meterNumbers = new ArrayList<>();
    CollectionReference metersRef;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private Bitmap photoBitmap;
    private ImageView imagePreview;
    private double latitude = 0.0;
    private double longitude = 0.0;

    private static final String CHANNEL_ID = "gazmester_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int DAILY_REMINDER_REQUEST_CODE = 200;
    private static final int JOB_ID_SYNC = 300;
    private static final String OFFLINE_FILE = "offline_readings.dat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_reading);

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 200);
            }
        }

        // MaterialToolbar beállítása egységesen a ReadingListActivity-hez
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        inputDatum = findViewById(R.id.inputDatum);
        inputDatum.setFocusable(false); // Ne nyíljon meg a billentyűzet
        inputDatum.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    NewReadingActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String formattedDate = String.format("%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                        inputDatum.setText(formattedDate);
                    },
                    year, month, day
            );

            datePickerDialog.show();
        });

        spinnerMeters = findViewById(R.id.spinnerMeters);
        metersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, meterNumbers);
        metersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeters.setAdapter(metersAdapter);

        imagePreview = findViewById(R.id.imagePreview);

        Log.d(LOG_TAG, "createNotificationChannel called");
        createNotificationChannel();
        scheduleDailyReminder();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            metersRef = db.collection("users").document(user.getUid()).collection("meters");
            loadMetersForSpinner();
        }
    }

    private void loadMetersForSpinner() {
        meterNumbers.clear();
        metersRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String meterNumber = doc.getString("meterNumber");
                if (meterNumber != null) meterNumbers.add(meterNumber);
            }
            metersAdapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ellenőrizzük, hogy a felhasználó be van-e jelentkezve
        if (auth == null) {
            auth = FirebaseAuth.getInstance();
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "A leolvasás csak bejelentkezett felhasználóknak elérhető!", Toast.LENGTH_SHORT).show();
            finish();
        }

        getLocation();

        // Helyes gázóra ellenőrzés: csak akkor jelezzen, ha tényleg nincs elem a meterNumbers-ben
        // Frissítsük a listát minden onStart-nál, és csak a callback-ben ellenőrizzük
        if (metersRef != null) {
            metersRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
                meterNumbers.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String meterNumber = doc.getString("meterNumber");
                    if (meterNumber != null) meterNumbers.add(meterNumber);
                }
                metersAdapter.notifyDataSetChanged();
                if (meterNumbers.isEmpty()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Nincs gázóra!")
                        .setMessage("A leolvasáshoz először adj hozzá egy gázórát a 'Gázórák kezelése' menüpontban!")
                        .setCancelable(false)
                        .setPositiveButton("Gázóra hozzáadása", (dialog, which) -> {
                            Intent intent = new Intent(this, MeterListActivity.class);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("Mégse", (dialog, which) -> finish())
                        .show();
                }
            });
        }
    }

    public void saveReport(android.view.View view) {
        btnMent = findViewById(R.id.btnMent);
        spinnerMeters = findViewById(R.id.spinnerMeters);
        inputOraAllas = findViewById(R.id.inputOraAllas);
        inputDatum = findViewById(R.id.inputDatum);
        String oraSzam = spinnerMeters.getSelectedItem() != null ? spinnerMeters.getSelectedItem().toString() : "";
        String oraAllas = inputOraAllas.getText().toString().trim();
        String datum = inputDatum.getText().toString().trim();

        if (spinnerMeters.getAdapter() == null || spinnerMeters.getAdapter().getCount() == 0) {
            Toast.makeText(this, "Először adj hozzá egy gázórát a lejelentéshez!", Toast.LENGTH_LONG).show();
            return;
        }
        if (oraSzam.isEmpty() || oraAllas.isEmpty() || datum.isEmpty()) {
            Toast.makeText(this, "Kérlek tölts ki minden mezőt!", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Megerősítés")
            .setMessage("Biztosan el akarod menteni az új leolvasást?")
            .setPositiveButton("Igen", (dialog, which) -> saveReportConfirmed(oraSzam, oraAllas, datum))
            .setNegativeButton("Mégse", null)
            .show();
    }

    private void saveReportConfirmed(String oraSzam, String oraAllas, String datum) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            return;
        }
        double allas;
        try {
            allas = Double.parseDouble(oraAllas);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Az óraállás csak szám lehet!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Fotó feltöltés helyett csak helyadatok és egyéb mezők mentése
        saveReadingToFirestore(user, oraSzam, allas, datum, null);
    }

    private void saveReadingToFirestore(FirebaseUser user, String oraSzam, double allas, String datum, String photoUrl) {
        GasMeterReading reading = new GasMeterReading(user.getUid(), oraSzam, allas, datum, null, latitude, longitude);
        db.collection("readings")
                .add(reading)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Sikeres mentés!", Toast.LENGTH_SHORT).show();
                    inputOraAllas.setText("");
                    inputDatum.setText("");
                    imagePreview.setImageBitmap(null);
                    photoBitmap = null;
                    sendSuccessNotification(); // Értesítés küldése
                })
                .addOnFailureListener(e -> {
                    // Offline mentés, ha nincs internet
                    saveReadingOffline(reading);
                    scheduleSyncJob();
                    Toast.makeText(this, "Hiba történt a mentéskor! Offline mentés.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSuccessNotification() {
        Log.d(LOG_TAG, "sendSuccessNotification called");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Sikeres mentés")
                .setContentText("A gázóra leolvasás elmentve.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(LOG_TAG, "NotificationManager is null");
            return;
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Gazmester értesítések";
            String description = "Értesítések a gázóra leolvasásokról";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, DailyReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, DAILY_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE);
        // Minden nap 18:00-kor
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long triggerTime = calendar.getTimeInMillis();
        if (System.currentTimeMillis() > triggerTime) {
            triggerTime += AlarmManager.INTERVAL_DAY;
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void saveReadingOffline(GasMeterReading reading) {
        try {
            List<GasMeterReading> readings = loadOfflineReadings();
            readings.add(reading);
            FileOutputStream fos = openFileOutput(OFFLINE_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(readings);
            oos.close();
            fos.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Offline mentés hiba", e);
        }
    }

    private List<GasMeterReading> loadOfflineReadings() {
        List<GasMeterReading> readings = new ArrayList<>();
        try {
            File file = new File(getFilesDir(), OFFLINE_FILE);
            if (!file.exists()) return readings;
            FileInputStream fis = openFileInput(OFFLINE_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
            readings = (List<GasMeterReading>) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Offline olvasás hiba", e);
        }
        return readings;
    }

    private void scheduleSyncJob() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID_SYNC, new android.content.ComponentName(this, SyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setBackoffCriteria(60_000, JobInfo.BACKOFF_POLICY_LINEAR)
                .build();
        jobScheduler.schedule(jobInfo);
    }

    public void onBackButtonClick(android.view.View view) {
        finish();
    }

    public void onTakePhotoClick(android.view.View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "A kamera engedély szükséges a fotó készítéséhez!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "A helymeghatározás engedély szükséges!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            photoBitmap = (Bitmap) extras.get("data");
            imagePreview.setImageBitmap(photoBitmap);
            // --- Gázóra fotó mentése az eszközre ---
            if (photoBitmap != null) {
                savePhotoToGallery(photoBitmap);
            }
        }
    }

    private void savePhotoToGallery(Bitmap bitmap) {
        String displayName = "gazora_" + System.currentTimeMillis() + ".jpg";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Gazmester");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                Toast.makeText(this, "Fotó elmentve a galériába!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Hiba a fotó mentésekor!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Régi Android verziókhoz
            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(), bitmap, displayName, "Gázóra fotó");
            if (savedImageURL != null) {
                Toast.makeText(this, "Fotó elmentve a galériába!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Hiba a fotó mentésekor!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Location permission error", e);
        }
    }
}