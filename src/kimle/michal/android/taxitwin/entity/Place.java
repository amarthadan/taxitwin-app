package kimle.michal.android.taxitwin.entity;

public class Place {

    private String address;
    private Long latitude;
    private Long longitude;

    public Place(String address, Long latitude, Long longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getLatitude() {
        return latitude;
    }

    public void setLatitude(Long latitude) {
        this.latitude = latitude;
    }

    public Long getLongitude() {
        return longitude;
    }

    public void setLongitude(Long longitude) {
        this.longitude = longitude;
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
}
