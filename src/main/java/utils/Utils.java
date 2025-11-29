package utils;

import gearth.protocol.HPacket;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static int extraSleepTime = 0;

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms + extraSleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void setExtraSleepTime(int extraSleepTime) {
        Utils.extraSleepTime = extraSleepTime;
    }

    public static List<Integer> readIntList(HPacket packet) {
        int size = packet.readInteger();
        List<Integer> intList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            intList.add(packet.readInteger());
        }

        return intList;
    }

    public static List<String> readStringList(HPacket packet) {
        int size = packet.readInteger();
        List<String> stringList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            stringList.add(packet.readString());
        }

        return stringList;
    }

    public static List<Long> readLongList(HPacket packet) {
        int size = packet.readInteger();
        List<Long> longList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            longList.add(packet.readLong());
        }

        return longList;
    }
}
