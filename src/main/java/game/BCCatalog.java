package game;

import extension.GPresets;
import extension.logger.Logger;
import furnidata.FurniDataTools;
import gearth.extensions.parsers.HProductType;
import gearth.extensions.parsers.catalog.HCatalogIndex;
import gearth.extensions.parsers.catalog.HCatalogPage;
import gearth.extensions.parsers.catalog.HCatalogPageIndex;
import gearth.extensions.parsers.catalog.HProduct;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import org.apache.commons.io.FileUtils;
import utils.Callback;
import utils.Utils;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BCCatalog {

    public static class SingleFurniProduct {
        private final int pageId;
        private final int offerId;
        private final String extraParam;

        SingleFurniProduct(int pageId, int offerId, String extraParam) {
            this.pageId = pageId;
            this.offerId = offerId;
            this.extraParam = extraParam;
        }

        public int getPageId() {
            return pageId;
        }

        public int getOfferId() {
            return offerId;
        }

        public String getExtraParam() {
            return extraParam;
        }
    }

    public enum CatalogState {
        NONE,
        AWAITING_INDEX,
        COLLECTING_PAGES,
        COLLECTED
    }

    // todo maybe handling catalog updates

    private Logger logger;
    private Callback stateChangeCallback;

    private final GPresets extension;
    private CatalogState state = CatalogState.NONE;

    private final Map<Integer, SingleFurniProduct> typeIdToProduct = new HashMap<>();

    public BCCatalog(GPresets extension, Logger logger, Callback stateChangeCallback) {
        this.extension = extension;
        this.logger = logger;
        this.stateChangeCallback = stateChangeCallback;

        extension.intercept(HMessage.Direction.TOCLIENT, "CatalogIndex", this::onCatalogIndex);
        extension.intercept(HMessage.Direction.TOSERVER, "GetCatalogPage", (m) -> {
            if (state == CatalogState.COLLECTING_PAGES) {
                m.setBlocked(true);
            }
        });
        extension.intercept(HMessage.Direction.TOCLIENT, "CatalogPage", this::onCatalogPage);
    }

    private void onCatalogPage(HMessage hMessage) {
        if (state == CatalogState.COLLECTING_PAGES) {
            HCatalogPage page = new HCatalogPage(hMessage.getPacket());
            page.getOffers().forEach(catalogOffer -> {
                if (!catalogOffer.isPet() && catalogOffer.getProducts().size() == 1) {
                    HProduct product = catalogOffer.getProducts().get(0);
                    if(product.getProductType() == HProductType.FloorItem) {
                        synchronized (typeIdToProduct) {
                            typeIdToProduct.put(product.getFurniClassId(), new SingleFurniProduct(
                                    page.getPageId(),
                                    catalogOffer.getOfferId(),
                                    product.getExtraParam()
                            ));
                        }
                    }
                }
            });
        }
    }

    public SingleFurniProduct getProductFromTypeId(int typeId) {
        return typeIdToProduct.get(typeId);
    }

    private void fetchPagesLoop(List<Integer> pageIds, String saveHash) {
        logger.log(String.format("Scraping %d BC catalog pages..", pageIds.size()), "blue");

        int i = 0;
        while (state == CatalogState.COLLECTING_PAGES && i < pageIds.size()) {
            extension.sendToServer(new HPacket("GetCatalogPage", HMessage.Direction.TOSERVER,
                    pageIds.get(i), -1, "BUILDERS_CLUB"));

            Utils.sleep(180);

            i++;
            if (i % 10 == 0) {
                logger.log(String.format("Scraping page %d of %d", i, pageIds.size()), "blue");
            }
        }

        if (state == CatalogState.COLLECTING_PAGES) {
            new Thread(() -> {
                Utils.sleep(300);
                if (state == CatalogState.COLLECTING_PAGES) setState(CatalogState.COLLECTED);
                logger.log(String.format("Collected %d BC products", typeIdToProduct.size()), "blue");

                saveCatalog(saveHash);
            }).start();
        }

    }
    private void findRelevantPageIds(List<Integer> pageIds, HCatalogPageIndex node) {
        if (node.getPageId() != -1 && node.getOfferIds().size() > 0) {
            pageIds.add(node.getPageId());
        }
        node.getChildren().forEach((s) -> findRelevantPageIds(pageIds, s));
    }

    private void onCatalogIndex(HMessage hMessage) {
        if (state == CatalogState.AWAITING_INDEX) {
            HCatalogIndex index = new HCatalogIndex(hMessage.getPacket());
            if (index.getCatalogType().equals("BUILDERS_CLUB")) {
                List<Integer> relevantPageIds = new ArrayList<>();
                findRelevantPageIds(relevantPageIds, index.getRoot());

                setState(CatalogState.COLLECTING_PAGES);
                String hash = ""+relevantPageIds.stream().map(integer -> ""+integer).collect(Collectors.joining(",")).hashCode();
                if (loadCatalog(hash)) {
                    setState(CatalogState.COLLECTED);
                    logger.log(String.format("Loaded %d BC products from cache", typeIdToProduct.size()), "blue");
                }
                else {
                    new Thread(() -> fetchPagesLoop(relevantPageIds, hash)).start();
                }
            }
        }
    }
    public void requestIndex() {
        clear();
        setState(CatalogState.AWAITING_INDEX);
        extension.sendToServer(new HPacket("GetCatalogIndex", HMessage.Direction.TOSERVER, "BUILDERS_CLUB"));
        stateChangeCallback.call();
    }

    private void setState(CatalogState state) {
        this.state = state;
        stateChangeCallback.call();
    }
    public void clear() {
        setState(CatalogState.NONE);
        typeIdToProduct.clear();
        stateChangeCallback.call();
    }

    public CatalogState getState() {
        return state;
    }


    private String fileNameForHash(String hash) throws URISyntaxException {
        String path = (new File(GPresets.class.getProtectionDomain().getCodeSource().getLocation().toURI()))
                .getParentFile().toString();
        String filename = "BC_FLOORITEMS_" + hash + ".txt";

        File root = new File(Paths.get(path, "catalog").toString());
        if (!root.exists()) {
            root.mkdir();
        }

        return Paths.get(path, "catalog", filename).toString();
    }

    // return true if it worked
    private boolean saveCatalog(String hash) {
        if (state == CatalogState.COLLECTED && extension.furniDataReady()) {
            FurniDataTools furniDataTools = extension.getFurniDataTools();


            StringBuilder builder = new StringBuilder();
            for(Integer typeId : typeIdToProduct.keySet()) {
                SingleFurniProduct product = typeIdToProduct.get(typeId);
                String furniName = furniDataTools.getFloorItemName(typeId);
                String entry = product.extraParam.isEmpty() ?
                        String.format("%s\t%d\t%d", furniName, product.pageId, product.offerId) :
                        String.format("%s\t%d\t%d\t%s", furniName, product.pageId, product.offerId, product.extraParam);

                builder.append(entry);
                builder.append("\n");
            }

            try {
                FileUtils.writeStringToFile(new File(fileNameForHash(hash)), builder.toString(), StandardCharsets.UTF_8);
                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    // return true if it worked
    private boolean loadCatalog(String hash) {
        if (state == CatalogState.COLLECTING_PAGES && extension.furniDataReady()) {
            FurniDataTools furniDataTools = extension.getFurniDataTools();

            try {
                File file = new File(fileNameForHash(hash));
                if (file.exists() && !file.isDirectory()) {
                    String[] allLines = FileUtils.readFileToString(file, StandardCharsets.UTF_8).split("\n");
                    for(String line : allLines) {
                        if (!line.isEmpty()) {
                            String[] fields = line.split("\t");
                            int typeId = furniDataTools.getFloorTypeId(fields[0]);
                            int pageId = Integer.parseInt(fields[1]);
                            int offerId = Integer.parseInt(fields[2]);
                            String extraParam = "";
                            if (fields.length == 4) {
                                extraParam = fields[3];
                            }

                            typeIdToProduct.put(typeId, new SingleFurniProduct(pageId, offerId, extraParam));
                        }
                    }

                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void clearCache() throws URISyntaxException {
        String path = (new File(GPresets.class.getProtectionDomain().getCodeSource().getLocation().toURI()))
                .getParentFile().toString();
        File root = new File(Paths.get(path, "catalog").toString());
        if (root.exists() && root.listFiles() != null) {
            for (File file : root.listFiles()) {
                System.out.println(file);
                file.delete();
            }
        }
    }
}
