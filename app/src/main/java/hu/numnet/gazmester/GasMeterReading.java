package hu.numnet.gazmester;

public class GasMeterReading {
    private String userId;
    private String meterNumber;
    private double meterValue;
    private String date;
    private String photoUrl;
    private double latitude;
    private double longitude;

    public GasMeterReading() {}

    public GasMeterReading(String userId, String meterNumber, double meterValue, String date, String photoUrl, double latitude, double longitude) {
        this.userId = userId;
        this.meterNumber = meterNumber;
        this.meterValue = meterValue;
        this.date = date;
        this.photoUrl = photoUrl;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMeterNumber() { return meterNumber; }
    public void setMeterNumber(String meterNumber) { this.meterNumber = meterNumber; }

    public double getMeterValue() { return meterValue; }
    public void setMeterValue(double meterValue) { this.meterValue = meterValue; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
