package extension.tools;

import extension.tools.presetconfig.PresetConfig;
import extension.tools.presetconfig.furni.PresetFurni;
import game.FloorState;
import gearth.extensions.parsers.HPoint;

import java.util.List;

public class PresetUtils {

    public static HPoint presetDimensions(PresetConfig presetConfig) {
        List<PresetFurni> furni = presetConfig.getFurniture();

        if (furni.size() == 0) return new HPoint(0, 0);

        int lowestX = furni.get(0).getLocation().getX();
        int lowestY = furni.get(0).getLocation().getY();
        int highestX = lowestX;
        int highestY = lowestY;

        for (PresetFurni f : furni) {
            HPoint loc = f.getLocation();
            if (loc.getX() < lowestX) lowestX = loc.getX();
            if (loc.getY() < lowestY) lowestY = loc.getY();
            if (loc.getX() > highestX) highestX = loc.getX();
            if (loc.getY() > highestY) highestY = loc.getY();
        }

        return new HPoint(highestX - lowestX + 1, highestY - lowestY + 1);
    }

    public static int heightFromChar(char x) {
        if (x == 'x') return 256;

        if (Character.isDigit(x)) {
            return x - '0';
        }
        else if (Character.isLetter(x) && Character.isLowerCase(x)) {
            return x - 'a' + 10;
        }
        
        return 256;
    }

    public static int lowestFloorPoint(FloorState floor, PresetConfig presetConfig, HPoint presetRoot) {
        HPoint dim = presetDimensions(presetConfig);
        
        return lowestFloorPoint(
                floor,
                presetRoot,
                new HPoint(
                        presetRoot.getX() + dim.getX(),
                        presetRoot.getY() + dim.getY()
                )
        );
    }

    public static int lowestFloorPoint(FloorState floor, HPoint start, HPoint end) {
        int lowestPoint = 256;

        for (int x = start.getX(); x < end.getX(); x++) {
            for (int y = start.getY(); y < end.getY(); y++) {
                int height = heightFromChar(floor.floorHeight(x, y));
                if (height < lowestPoint) {
                    lowestPoint = height;
                }
            }
        }

        return lowestPoint;
    }

}
