package utils;

import gearth.extensions.parsers.HFloorItem;

public class StateExtractor {

    // return null if no positive integer state (wired configurable) can be extracted.
    // State is returned in string even though it should be int parsable
    public static String stateFromItem(HFloorItem floorItem) {
        String stateAsString = null;
        if (floorItem.getUsagePolicy() < 1) {
            return null;
        }

        if (floorItem.getCategory() == 0) { //legacy
            stateAsString = (String)(floorItem.getStuff()[0]);
        }
        else if (floorItem.getCategory() == 1) { //mapdata
            int count = (int)(floorItem.getStuff()[0]);
            for (int i = 0; i < count; i++) {
                String key = (String)(floorItem.getStuff()[i*2 + 1]);
                String value = (String)(floorItem.getStuff()[i*2 + 2]);

                if (key.equals("state")) {
                    stateAsString = value;
                    break;
                }
            }
        }

        if (stateAsString != null) {
            try {
                if (Integer.parseInt(stateAsString) >= 0) {
                    return stateAsString;
                }
            }
            catch (Exception ignore) {  }
        }
        return null;

    }

}
