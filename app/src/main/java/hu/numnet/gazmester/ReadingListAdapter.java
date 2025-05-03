package hu.numnet.gazmester;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReadingListAdapter extends RecyclerView.Adapter<ReadingListAdapter.ViewHolder> {
    private List<GasMeterReading> readings;
    private OnDeleteClickListener onDeleteClickListener;

    public ReadingListAdapter(List<GasMeterReading> readings) {
        this.readings = readings;
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(GasMeterReading reading, int position);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reading, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GasMeterReading reading = readings.get(position);
        // Format date
        String formattedDate = reading.getDate();
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(reading.getDate());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy. MMMM d.", Locale.getDefault());
            formattedDate = outputFormat.format(date);
        } catch (ParseException ignored) {}
        holder.textDate.setText("Dátum: " + formattedDate);
        holder.textMeterNumber.setText("Óraszám: " + reading.getMeterNumber());
        holder.textMeterValue.setText("Óraállás: " + NumberFormat.getInstance().format(reading.getMeterValue()) + " m³");
        // Animate appearance
        Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), android.R.anim.fade_in);
        holder.itemView.startAnimation(animation);
        // Set delete button click
        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(reading, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return readings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDate, textMeterNumber, textMeterValue;
        View btnDelete;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textMeterNumber = itemView.findViewById(R.id.textMeterNumber);
            textMeterValue = itemView.findViewById(R.id.textMeterValue);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
