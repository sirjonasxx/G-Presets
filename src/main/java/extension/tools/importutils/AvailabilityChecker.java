package extension.tools.importutils;

import extension.logger.Logger;
import extension.tools.postconfig.ItemSource;
import furnidata.FurniDataTools;
import game.BCCatalog;
import game.FloorState;
import game.Inventory;
import gearth.extensions.parsers.HFloorItem;
import gearth.extensions.parsers.HInventoryItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AvailabilityChecker {

    // returns null if invalid resources to check availability
    // return map of counts of missing furniture
    // map is empty if you can start importing
    public static Map<String, Integer> missingItems(List<FurniDropInfo> furniDrops, Inventory inventory, BCCatalog catalog, FurniDataTools furniDataTools, FloorState floorState, boolean useRoomFurni) {

        if (inventory.getState() == Inventory.InventoryState.LOADED
                && catalog.getState() == BCCatalog.CatalogState.COLLECTED
                && furniDataTools.isReady()) {

            Map<Integer, Integer> usedInventorySpots = new HashMap<>();
            Map<Integer, Integer> missingItemCounts = new HashMap<>();
            for (FurniDropInfo furniDropInfo : furniDrops) {
                int typeId = furniDropInfo.getTypeId();
                usedInventorySpots.putIfAbsent(typeId, 0);
                ItemSource src = furniDropInfo.getItemSource();

                List<HInventoryItem> invItems = inventory.getFloorItemsByType(typeId);
                boolean isMissing = false;
                boolean useInventorySpace = false;

                if (src == ItemSource.ONLY_BC) {
                    if (catalog.getProductFromTypeId(typeId) == null) isMissing = true;
                }
                else if (src == ItemSource.PREFER_BC) {
                    if (catalog.getProductFromTypeId(typeId) == null) {
                        if (usedInventorySpots.get(typeId) < invItems.size()) useInventorySpace = true;
                        else isMissing = true;
                    }
                }
                else if (src == ItemSource.PREFER_INVENTORY) {
                    if (usedInventorySpots.get(typeId) < invItems.size()) useInventorySpace = true;
                    else isMissing = catalog.getProductFromTypeId(typeId) == null;
                }
                else if (src == ItemSource.ONLY_INVENTORY) {
                    if (usedInventorySpots.get(typeId) < invItems.size()) useInventorySpace = true;
                    else isMissing = true;
                }


                if(isMissing && useRoomFurni) {
                    List<HFloorItem> itemsInFloor = floorState.getItemsFromType(typeId);
                    long roomItemCount = itemsInFloor
                            .stream()
                            .filter(furni -> furni.getTile().getX() != furniDropInfo.getX() && furni.getTile().getY() != furniDropInfo.getY())
                            .count();
                    if (usedInventorySpots.get(typeId) < roomItemCount ) {
                        useInventorySpace = true;
                        isMissing = false;
                    }
                }

                if (isMissing) {
                    missingItemCounts.putIfAbsent(typeId, 0);
                    missingItemCounts.put(typeId, missingItemCounts.get(typeId) + 1);
                }
                if (useInventorySpace) {
                    usedInventorySpots.put(typeId, usedInventorySpots.get(typeId) + 1);
                }
            }

            Map<String, Integer> missingItemCountsByName = new HashMap<>();

            for (int typeId : missingItemCounts.keySet()) {
                String className = furniDataTools.getFloorItemName(typeId);
                missingItemCountsByName.put(className, missingItemCounts.get(typeId));
            }

            return missingItemCountsByName;
        }
        else return null;
    }

    public static void printAvailability(Logger logger, List<FurniDropInfo> furniDrops, Inventory inventory, BCCatalog catalog, FurniDataTools furniDataTools, FloorState floorState, boolean useRoomFurni) {

        Map<String, Integer> missing = missingItems(furniDrops, inventory, catalog, furniDataTools, floorState, useRoomFurni);
        if (inventory.getState() == Inventory.InventoryState.LOADED
                && catalog.getState() == BCCatalog.CatalogState.COLLECTED
                && furniDataTools.isReady() && missing != null) {

            List<String> allItems = furniDrops.stream()
                    .map(i -> furniDataTools.getFloorItemName(i.getTypeId()))
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            logger.log("Required furniture: ", "black");
            for (String className : allItems) {
                boolean isMissing = missing.containsKey(className) && missing.get(className) > 0;
                int totalNeeded = (int)(furniDrops.stream().filter(i -> furniDataTools.getFloorItemName(i.getTypeId()).equals(className)).count());
                int available = isMissing ? totalNeeded - missing.get(className) : totalNeeded;

                logger.logNoNewline(String.format("* %s ", furniDataTools.getFloorItemDetails(className).name), "black");
                logger.log(String.format("(%d/%d)", available, totalNeeded), isMissing ? "red" : "green");
            }

        }
        else {
            logger.log("Availability check failed, check if everything is loaded", "red");
        }
    }

}
