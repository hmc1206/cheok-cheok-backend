package com.chuckchuck.map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverGeocodeResponse {
    private String status;
    private List<AddressItem> addresses;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<AddressItem> getAddresses() { return addresses; }
    public void setAddresses(List<AddressItem> addresses) { this.addresses = addresses; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddressItem {
        private String x; // 경도 (Longitude)
        private String y; // 위도 (Latitude)

        public String getX() { return x; }
        public void setX(String x) { this.x = x; }
        public String getY() { return y; }
        public void setY(String y) { this.y = y; }
    }
}
