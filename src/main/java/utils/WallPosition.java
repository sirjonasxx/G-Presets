package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WallPosition {
    private static final Pattern pattern = Pattern.compile("w=(?<x>\\d+),(?<y>\\d+) l=(?<offsetX>\\d+),(?<offsetY>\\d+) (?<direction>[rl])( a=(?<altitude>\\d+))?");

    private final int x;
    private final int y;
    private final int offsetX;
    private final int offsetY;
    private final String direction;
    private final int altitude;

    public WallPosition(int x, int y, int offsetX, int offsetY, String direction, int altitude) {
        this.x = x;
        this.y = y;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.direction = direction;
        this.altitude = altitude;
    }

    public WallPosition(String positionString) {
        Matcher matcher = pattern.matcher(positionString);
        if (matcher.find()) {
            this.x = Integer.parseInt(matcher.group("x"));
            this.y = Integer.parseInt(matcher.group("y"));
            this.offsetX = Integer.parseInt(matcher.group("offsetX"));
            this.offsetY = Integer.parseInt(matcher.group("offsetY"));
            this.direction = matcher.group("direction");
            String altitudeGroup = matcher.group("altitude");
            this.altitude = altitudeGroup == null ? -1 : Integer.parseInt(altitudeGroup);
        } else {
            throw new IllegalArgumentException("Invalid position string: " + positionString);
        }
    }

    public String toString() {
        return String.format(":w=%d,%d l=%d,%d %s a=%d", x, y, offsetX, offsetY, direction, altitude);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public String getDirection() {
        return direction;
    }

    public int getAltitude() {
        return altitude;
    }
}
