package com.example.fancontroller;

public class IvecoData {
    private String gearPos;
    private String inRpm;
    private String outRpm;

    public IvecoData(String gearPos, String inRpm, String outRpm) {
        this.gearPos = gearPos;
        this.inRpm = inRpm;
        this.outRpm = outRpm;
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
}
