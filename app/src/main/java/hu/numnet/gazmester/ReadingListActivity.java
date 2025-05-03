package hu.numnet.gazmester;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Collections;
import java.util.List;

public class ReadingListActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ReadingAdapter adapter;
    private List<GasMeterReading> readingList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_list);

        // Set up MaterialToolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the Up button
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
    }

    // Handle Up button press
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
                    // Sort by date descending (newest first)
                    Collections.sort(readingList, (a, b) -> b.getDate().compareTo(a.getDate()));
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ReadingListActivity", "Hiba a leolvasások lekérdezésekor", e));
    }

    // RecyclerView Adapter
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

    // TODO: Szerkesztés és törlés funkciók hozzáadása
}
