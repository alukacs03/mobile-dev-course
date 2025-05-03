package hu.numnet.gazmester;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;

public class SyncJobService extends JobService {
    private static final String LOG_TAG = "SyncJobService";
    private static final String OFFLINE_FILE = "offline_readings.dat";

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                File file = new File(getFilesDir(), OFFLINE_FILE);
                if (!file.exists()) {
                    jobFinished(params, false);
                    return;
                }
                List<GasMeterReading> readings = new ArrayList<>();
                FileInputStream fis = openFileInput(OFFLINE_FILE);
                ObjectInputStream ois = new ObjectInputStream(fis);
                readings = (List<GasMeterReading>) ois.readObject();
                ois.close();
                fis.close();
                if (readings.isEmpty()) {
                    file.delete();
                    jobFinished(params, false);
                    return;
                }
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                for (GasMeterReading reading : readings) {
                    db.collection("readings").add(reading)
                        .addOnSuccessListener(documentReference -> Log.d(LOG_TAG, "Szinkronizált leolvasás."))
                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Szinkronizáció hiba", e));
                }
                file.delete();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Szinkronizáció hiba", e);
            }
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
