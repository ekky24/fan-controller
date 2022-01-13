package com.example.fancontroller;

public class IvecoData {
    private String gearPos;
    private String inRpm;
    private String outRpm;
    private String timestamp;

    public IvecoData(String gearPos, String inRpm, String outRpm, String timestamp) {
        this.gearPos = gearPos;
        this.inRpm = inRpm;
        this.outRpm = outRpm;
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

    public String getTimestamp() {
        return timestamp;
    }
}
