package utils;

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
}
