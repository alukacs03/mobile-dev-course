package hu.numnet.gazmester;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.chip.Chip;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReadingListActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ReadingAdapter adapter;
    private List<GasMeterReading> readingList = new ArrayList<>();
    private Date selectedDate;
    private Date startDate;
    private Date endDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = findViewById(R.id.readingRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReadingAdapter(readingList);
        recyclerView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserReadings();

        findViewById(R.id.startDatePickerET).setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                startDate = calendar.getTime();
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String formattedDate = dateFormat.format(startDate);
                ((EditText) findViewById(R.id.startDatePickerET)).setText(formattedDate);
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        findViewById(R.id.endDatePickerET).setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                endDate = calendar.getTime();
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String formattedDate = dateFormat.format(endDate);
                ((EditText) findViewById(R.id.endDatePickerET)).setText(formattedDate);
            }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        findViewById(R.id.btnFilterReadings).setOnClickListener(v -> {
            if (startDate != null && endDate != null) {
                loadFilteredReadingsBetweenDates(startDate, endDate);
            } else if (selectedDate != null) {
                loadFilteredReadingsFromDate(selectedDate);
            } else {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadUserReadings() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db.collection("readings")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    readingList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GasMeterReading reading = doc.toObject(GasMeterReading.class);
                        readingList.add(reading);
                    }
                    Collections.sort(readingList, (a, b) -> b.getDate().compareTo(a.getDate()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ReadingListActivity", "Hiba a leolvasások lekérdezésekor", e));
    }

    private void loadFilteredReadings(int daysInPast) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        long cutoffDateMillis = System.currentTimeMillis() - (daysInPast * 24L * 60 * 60 * 1000);
        com.google.firebase.Timestamp cutoffDate = new com.google.firebase.Timestamp(new java.util.Date(cutoffDateMillis));
        db.collection("readings")
                .whereEqualTo("userId", user.getUid())
                .whereGreaterThanOrEqualTo("date", cutoffDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    readingList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GasMeterReading reading = doc.toObject(GasMeterReading.class);
                        readingList.add(reading);
                    }
                    // Sort by date descending (newest first)
                    Collections.sort(readingList, (a, b) -> b.getDate().compareTo(a.getDate()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ReadingListActivity", "Hiba a leolvasások lekérdezésekor", e));
    }

    private void loadFilteredReadingsFromDate(Date selectedDate) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d("ReadingListActivity", "Selected date: " + selectedDate);
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String selectedDateString = dateFormat.format(selectedDate);
        Log.d("ReadingListActivity", "Selected date string: " + selectedDateString);
        db.collection("readings")
                .whereEqualTo("userId", user.getUid())
                .whereGreaterThanOrEqualTo("date", selectedDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("ReadingListActivity", "Query successful, document count: " + queryDocumentSnapshots.size());
                    readingList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GasMeterReading reading = doc.toObject(GasMeterReading.class);
                        Log.d("ReadingListActivity", "Reading fetched: " + reading);
                        readingList.add(reading);
                    }
                    // Sort by date descending (newest first)
                    Collections.sort(readingList, (a, b) -> b.getDate().compareTo(a.getDate()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ReadingListActivity", "Query failed", e));
    }

    private void loadFilteredReadingsBetweenDates(Date startDate, Date endDate) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nincs bejelentkezve!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String startDateString = dateFormat.format(startDate);
        String endDateString = dateFormat.format(endDate);

        db.collection("readings")
                .whereEqualTo("userId", user.getUid())
                .whereGreaterThanOrEqualTo("date", startDateString)
                .whereLessThanOrEqualTo("date", endDateString)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    readingList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GasMeterReading reading = doc.toObject(GasMeterReading.class);
                        readingList.add(reading);
                    }
                    Collections.sort(readingList, (a, b) -> b.getDate().compareTo(a.getDate()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ReadingListActivity", "Query failed", e));
    }

    private static class ReadingAdapter extends RecyclerView.Adapter<ReadingAdapter.ViewHolder> {
        private List<GasMeterReading> readings;

        public ReadingAdapter(List<GasMeterReading> readings) {
            this.readings = readings;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reading_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GasMeterReading reading = readings.get(position);
            holder.chipMeterNumber.setText("Óraszám: " + reading.getMeterNumber());
            holder.textReadingDate.setText(reading.getDate());
            holder.textMeterValue.setText(reading.getMeterValue() + " m³");
        }

        @Override
        public int getItemCount() {
            return readings.size();
        }

        // ViewHolder class
        public static class ViewHolder extends RecyclerView.ViewHolder {
            com.google.android.material.chip.Chip chipMeterNumber;
            TextView textReadingDate, textMeterValue;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                chipMeterNumber = itemView.findViewById(R.id.chipMeterNumber);
                textReadingDate = itemView.findViewById(R.id.textReadingDate);
                textMeterValue = itemView.findViewById(R.id.textMeterValue);
            }
        }
    }

}
