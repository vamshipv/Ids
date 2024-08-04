package de.uniba.rz.entities.udp;

import java.io.Serializable;

public class Packet implements Serializable {

    private String type;
    private Object data;

    public Packet(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "type='" + type + '\'' +
                ", data=" + data +
                '}';
    }
}