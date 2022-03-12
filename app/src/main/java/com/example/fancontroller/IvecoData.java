package com.example.fancontroller;

public class IvecoData {
    private String gearPos;
    private String inRpm;
    private String outRpm;
    private String temp;
    private String pressure1;
    private String pressure2;
    private String pressure3;
    private String pressure4;
    private String timestamp;

    public IvecoData(String gearPos, String inRpm, String outRpm, String temp, String pressure1,
                     String pressure2, String pressure3, String pressure4, String timestamp) {
        this.gearPos = gearPos;
        this.inRpm = inRpm;
        this.outRpm = outRpm;
        this.temp = temp;
        this.pressure1 = pressure1;
        this.pressure2 = pressure2;
        this.pressure3 = pressure3;
        this.pressure4 = pressure4;
        this.timestamp = timestamp;
    }

    public String getGearPos() {
        return gearPos;
    }

    public String getInRpm() {
        return inRpm;
    }

    public String getOutRpm() {
        return outRpm;
    }

    public String getTemp() {
        return temp;
    }

    public String getPressure1() {
        return pressure1;
    }

    public String getPressure2() {
        return pressure2;
    }

    public String getPressure3() {
        return pressure3;
    }

    public String getPressure4() {
        return pressure4;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
