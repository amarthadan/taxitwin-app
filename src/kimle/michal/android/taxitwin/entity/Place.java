package kimle.michal.android.taxitwin.entity;

import android.os.Parcel;
import android.os.Parcelable;

public class Place implements Parcelable {

    private String address;
    private Double latitude;
    private Double longitude;

    public Place() {
    }

    public Place(String address, Double latitude, Double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Place(Parcel in) {
        address = in.readString();
        longitude = in.readDouble();
        latitude = in.readDouble();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean isFilled() {
        return !(latitude == null || longitude == null || latitude == 0 || longitude == 0);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.latitude != null ? this.latitude.hashCode() : 0);
        hash = 53 * hash + (this.longitude != null ? this.longitude.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Place other = (Place) obj;
        if ((this.address == null) ? (other.address != null) : !this.address.equals(other.address)) {
            return false;
        }
        if (this.latitude != other.latitude && (this.latitude == null || !this.latitude.equals(other.latitude))) {
            return false;
        }
        if (this.longitude != other.longitude && (this.longitude == null || !this.longitude.equals(other.longitude))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Place{" + "address=" + address + ", latitude=" + latitude + ", longitude=" + longitude + '}';
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(address);
    }

    public static final Parcelable.Creator<Place> CREATOR
            = new Parcelable.Creator<Place>() {
                public Place createFromParcel(Parcel in) {
                    return new Place(in);
                }

                public Place[] newArray(int size) {
                    return new Place[size];
                }
            };

}
