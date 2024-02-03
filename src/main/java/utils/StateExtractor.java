package utils;

import gearth.extensions.parsers.HFloorItem;

public class StateExtractor {

    // return null if no positive integer state (wired configurable) can be extracted.
    // State is returned in string even though it should be int parsable
    public static String stateFromItem(HFloorItem floorItem) {
        if (floorItem.getUsagePolicy() < 1) {
            return null;
        }

        int state = floorItem.getStuff().getState();
        if (state < 0) {
            return null;
        }

        return Integer.toString(state);
    }

}
