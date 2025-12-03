package kinoko.server.dialog.miniroom;

import kinoko.database.DatabaseManager;
import kinoko.packet.field.FieldPacket;
import kinoko.packet.field.MiniRoomPacket;
import kinoko.packet.world.WvsContext;
import kinoko.provider.ItemProvider;
import kinoko.provider.item.ItemInfo;
import kinoko.server.packet.InPacket;
import kinoko.world.GameConstants;
import kinoko.world.item.InventoryManager;
import kinoko.world.item.InventoryOperation;
import kinoko.world.item.InventoryType;
import kinoko.world.item.Item;
import kinoko.world.user.User;
import kinoko.world.user.stat.Stat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public final class EntrustedShop extends MiniRoom {
    private final String employerName;
    private final int employerId;
    private final int templateId;
    private final List<PlayerShopItem> items = new ArrayList<>();
    private final List<String> blockedList = new ArrayList<>();
    private final List<String> visitList = new ArrayList<>();
    private final List<SoldItemRecord> soldItems = new ArrayList<>(); // Track sold items for history
    private int foothold;
    private int money = 0;
    private boolean open = false;
    private Instant openTime;           // When the shop was opened
    private Instant expirationTime;     // When the shop expires
    private ScheduledFuture<?> expirationTask; // Task to close shop on expiration

    public EntrustedShop(String title, String password, String employerName, int employerId, int templateId) {
        super(title, password, 0);
        this.employerName = employerName;
        this.employerId = employerId;
        this.templateId = templateId;
    }

    public String getEmployerName() {
        return employerName;
    }

    public int getEmployerId() {
        return employerId;
    }

    public int getTemplateId() {
        return templateId;
    }

    public int getFoothold() {
        return foothold;
    }

    public void setFoothold(int foothold) {
        this.foothold = foothold;
    }

    public List<PlayerShopItem> getItems() {
        return items;
    }

    public List<String> getBlockedList() {
        return blockedList;
    }

    public List<String> getVisitList() {
        return visitList;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Instant getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Instant openTime) {
        this.openTime = openTime;
    }

    public Instant getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Instant expirationTime) {
        this.expirationTime = expirationTime;
    }

    public ScheduledFuture<?> getExpirationTask() {
        return expirationTask;
    }

    public void setExpirationTask(ScheduledFuture<?> expirationTask) {
        this.expirationTask = expirationTask;
    }

    public List<SoldItemRecord> getSoldItems() {
        return soldItems;
    }

    /**
     * Get time elapsed since shop opened in seconds.
     * Used for tPass field in packets.
     */
    public int getTimePassedSeconds() {
        if (openTime == null) {
            return 0;
        }
        return (int) ChronoUnit.SECONDS.between(openTime, Instant.now());
    }

    /**
     * Get remaining time until expiration in seconds.
     */
    public long getRemainingTimeSeconds() {
        if (expirationTime == null) {
            return 0;
        }
        long remaining = ChronoUnit.SECONDS.between(Instant.now(), expirationTime);
        return Math.max(0, remaining);
    }

    /**
     * Check if shop has expired.
     */
    public boolean isExpired() {
        return expirationTime != null && Instant.now().isAfter(expirationTime);
    }

    public int getOpenUserIndex() {
        // Start from 1 since index 0 is reserved for owner
        for (int i = 1; i < getMaxUsers(); i++) {
            if (!getUsers().containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public MiniRoomType getType() {
        return MiniRoomType.EntrustedShop;
    }

    @Override
    public int getMaxUsers() {
        return 4; // Owner at index 0, up to 3 visitors at indices 1-3
    }

    @Override
    public void handlePacket(User user, MiniRoomProtocol mrp, InPacket inPacket) {
        switch (mrp) {
            case ESP_PutItem -> {
                final int targetType = inPacket.decodeByte(); // nTI
                final int targetPosition = inPacket.decodeShort(); // nPos
                final int setCount = inPacket.decodeShort(); // nCount / nSet
                final int setSize = inPacket.decodeShort(); // nSet
                final int price = inPacket.decodeInt(); // nPrice
                // Validate action
                final long totalPrice = ((long) price * setCount);
                final InventoryType inventoryType = InventoryType.getByValue(targetType);
                if (inventoryType == null || inventoryType == InventoryType.EQUIPPED ||
                        targetPosition < 0 || setCount <= 0 || setSize <= 0 ||
                        price <= 0 || totalPrice <= 0 || totalPrice > Integer.MAX_VALUE ||
                        items.size() >= GameConstants.PLAYER_SHOP_SLOT_MAX ||
                        isOpen() || !isOwner(user)) {
                    log.error("Received invalid entrusted shop action {}", mrp);
                    user.dispose();
                    return;
                }
                // Resolve item
                final int totalCount = setCount * setSize;
                final Item item = user.getInventoryManager().getInventoryByType(inventoryType).getItem(targetPosition);
                if (item == null || item.getQuantity() < totalCount) {
                    log.error("Could not resolve item in inventory type {} position {} for entrusted shop action {}", inventoryType, targetPosition, mrp);
                    user.dispose();
                    return;
                }
                final Optional<ItemInfo> itemInfoResult = ItemProvider.getItemInfo(item.getItemId());
                if (itemInfoResult.isEmpty()) {
                    log.error("Could not resolve item info for item ID : {}", item.getItemId());
                    user.dispose();
                    return;
                }
                final ItemInfo itemInfo = itemInfoResult.get();
                if (itemInfo.isTradeBlock(item) || itemInfo.isAccountSharable()) {
                    log.error("Tried to put an untradable item into entrusted shop");
                    user.dispose();
                    return;
                }

                // Move item from inventory to shop
                final Optional<InventoryOperation> removeItemResult = user.getInventoryManager().removeItem(targetPosition, item, totalCount);
                if (removeItemResult.isEmpty()) {
                    throw new IllegalStateException("Could not remove item from inventory");
                }
                // Create shop item - item.quantity = perBundle (setSize), bundles = setCount
                final Item shopItem = new Item(item);
                shopItem.setItemSn(user.getNextItemSn());
                shopItem.setQuantity((short) setSize);  // perBundle quantity stored in item
                items.add(new PlayerShopItem(shopItem, (short) setCount, price));
                user.write(WvsContext.inventoryOperation(removeItemResult.get(), true));
                // Use existing kinoko method for refresh
                user.write(MiniRoomPacket.PlayerShop.refreshEntrustedShop(money, items));
            }
            case ESP_BuyItem -> {
                final int itemIndex = inPacket.decodeByte(); // nIdx
                final int setCount = inPacket.decodeShort();
                inPacket.decodeInt(); // ItemCRC
                if (itemIndex < 0 || itemIndex >= items.size() || setCount <= 0 ||
                        !isOpen() || isOwner(user)) {
                    log.error("Received invalid entrusted shop action {}", mrp);
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.Unknown));
                    user.dispose();
                    return;
                }
                // Resolve item
                final InventoryManager im = user.getInventoryManager();
                final PlayerShopItem shopItem = items.get(itemIndex);
                // Check if enough bundles are available
                if (shopItem.bundles < setCount) {
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.NoSlot));
                    user.dispose();
                    return;
                }
                final int totalCount = shopItem.getSetSize() * setCount;  // perBundle * bundles bought
                if (totalCount <= 0 || !im.canAddItem(shopItem.getItem().getItemId(), totalCount)) {
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.NoSlot));
                    user.dispose();
                    return;
                }
                // Resolve price
                final long totalPrice = ((long) shopItem.getPrice() * setCount);
                if (totalPrice <= 0 || totalPrice > Integer.MAX_VALUE || !im.canAddMoney((int) -totalPrice)) {
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.NoMoney));
                    user.dispose();
                    return;
                }
                // Apply tax
                final int moneyAfterTax = GameConstants.getPersonalShopTax((int) totalPrice);
                // Check if shop can hold the money
                final long newShopMoney = (long) money + moneyAfterTax;
                if (newShopMoney > Integer.MAX_VALUE) {
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.HostTooMuchMoney));
                    user.dispose();
                    return;
                }

                final Item buyItem = new Item(shopItem.getItem());
                buyItem.setItemSn(user.getNextItemSn());
                buyItem.setQuantity((short) totalCount);

                if (!im.addMoney((int) -totalPrice)) {
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.NoMoney));
                    user.dispose();
                    return;
                }

                final Optional<List<InventoryOperation>> addItemResult = im.addItem(buyItem);
                if (addItemResult.isEmpty()) {
                    im.addMoney((int) totalPrice);
                    user.write(MiniRoomPacket.PlayerShop.buyResult(PlayerShopBuyResult.NoSlot));
                    user.dispose();
                    return;
                }

                // Add money to shop (not directly to owner since they might be offline)
                addMoney(moneyAfterTax);

                // Decrease available bundles
                shopItem.bundles -= setCount;

                // Record the sale for history (shown in owner's UI)
                // Note: Meso is tracked in the shop's 'money' variable, NOT saved to DB per-sale
                // This prevents meso duplication if owner withdraws while shop is open
                final SoldItemRecord soldRecord = new SoldItemRecord(
                        shopItem.getItem().getItemId(),
                        (short) totalCount,
                        moneyAfterTax,
                        user.getCharacterName(),
                        buyItem
                );
                soldItems.add(soldRecord);

                // Update clients
                user.write(WvsContext.statChanged(Stat.MONEY, im.getMoney(), false));
                user.write(WvsContext.inventoryOperation(addItemResult.get(), true));

                // Notify owner if online
                final User owner = getUser(0);
                if (owner != null) {
                    owner.write(MiniRoomPacket.PlayerShop.addSoldItem(itemIndex, setCount, user.getCharacterName()));
                }

                if (isNoMoreItem()) {
                    closeShop(MiniRoomLeaveType.NoMoreItem);
                } else {
                    broadcastPacket(MiniRoomPacket.PlayerShop.refreshEntrustedShop(money, items));
                }
            }
            case ESP_MoveItemToInventory -> {
                final int itemIndex = inPacket.decodeShort(); // nIdx
                if (itemIndex < 0 || itemIndex >= items.size() ||
                        isOpen() || !isOwner(user)) {
                    log.error("Received invalid entrusted shop action {}", mrp);
                    return;
                }
                final PlayerShopItem shopItem = items.remove(itemIndex);
                // Restore full quantity: bundles * perBundle
                final int totalQuantity = shopItem.bundles * shopItem.getSetSize();
                final Item returnItem = new Item(shopItem.getItem());
                returnItem.setQuantity((short) totalQuantity);
                final Optional<List<InventoryOperation>> addItemResult = user.getInventoryManager().addItem(returnItem);
                if (addItemResult.isEmpty()) {
                    throw new IllegalStateException("Could not add entrusted shop item to inventory");
                }
                user.write(WvsContext.inventoryOperation(addItemResult.get(), true));
                user.write(MiniRoomPacket.PlayerShop.moveItemToInventory(items.size(), itemIndex));
            }
            case ESP_WithdrawAll -> {
                if (!isOwner(user)) {
                    log.error("Received invalid entrusted shop action {} from non-owner", mrp);
                    return;
                }
                if (isOpen()) {
                    user.write(MiniRoomPacket.PlayerShop.withdrawAllResult(PlayerShopWithdrawResult.Unknown));
                    return;
                }
                if (items.isEmpty()) {
                    user.write(MiniRoomPacket.PlayerShop.withdrawAllResult(PlayerShopWithdrawResult.Nothing));
                    return;
                }
                // Check if user has enough inventory slots
                final InventoryManager im = user.getInventoryManager();
                final List<InventoryOperation> allOperations = new ArrayList<>();
                for (PlayerShopItem shopItem : items) {
                    if (shopItem.bundles <= 0) {
                        continue;
                    }
                    // Restore full quantity: bundles * perBundle
                    final int totalQuantity = shopItem.bundles * shopItem.getSetSize();
                    final Item returnItem = new Item(shopItem.getItem());
                    returnItem.setQuantity((short) totalQuantity);
                    final Optional<List<InventoryOperation>> addItemResult = im.addItem(returnItem);
                    if (addItemResult.isEmpty()) {
                        user.write(MiniRoomPacket.PlayerShop.withdrawAllResult(PlayerShopWithdrawResult.NoSlot));
                        return;
                    }
                    allOperations.addAll(addItemResult.get());
                }
                items.clear();
                user.write(WvsContext.inventoryOperation(allOperations, true));
                user.write(MiniRoomPacket.PlayerShop.withdrawAllResult(PlayerShopWithdrawResult.Success));
                user.write(MiniRoomPacket.PlayerShop.refreshEntrustedShop(money, items));
            }
            case ESP_WithdrawMoney -> {
                if (!isOwner(user)) {
                    log.error("Received invalid entrusted shop action {} from non-owner", mrp);
                    return;
                }
                if (money <= 0) {
                    return;
                }
                final InventoryManager im = user.getInventoryManager();
                if (!im.canAddMoney(money)) {
                    // Cannot add money due to cap
                    return;
                }
                im.addMoney(money);
                money = 0;
                user.write(WvsContext.statChanged(Stat.MONEY, im.getMoney(), false));
                user.write(MiniRoomPacket.PlayerShop.withdrawMoneyResult());
                user.write(MiniRoomPacket.PlayerShop.refreshEntrustedShop(money, items));
            }
            case ESP_ArrangeItem -> {
                if (!isOwner(user) || isOpen()) {
                    log.error("Received invalid entrusted shop action {}", mrp);
                    return;
                }
                // Remove items with 0 bundles
                items.removeIf(shopItem -> shopItem.bundles <= 0);
                user.write(MiniRoomPacket.PlayerShop.arrangeItem(money));
                user.write(MiniRoomPacket.PlayerShop.refreshEntrustedShop(money, items));
            }
            case ESP_DeliverBlackList -> {
                if (isOpen() || !isOwner(user) || items.isEmpty()) {
                    log.error("Received invalid entrusted shop action {}", mrp);
                    return;
                }
                final int size = inPacket.decodeShort();
                blockedList.clear();
                for (int i = 0; i < size; i++) {
                    blockedList.add(inPacket.decodeString());
                }
            }
            case ESP_AddBlackList -> {
                if (!isOwner(user)) {
                    return;
                }
                final String name = inPacket.decodeString();
                if (!blockedList.contains(name)) {
                    blockedList.add(name);
                }
            }
            case ESP_DeleteBlackList -> {
                if (!isOwner(user)) {
                    return;
                }
                final String name = inPacket.decodeString();
                blockedList.remove(name);
            }
            case ESP_GoOut -> {
                // Owner temporarily leaving the shop (shop stays open)
                if (!isOwner(user)) {
                    log.error("Received ESP_GoOut from non-owner");
                    return;
                }
                // Remove owner from dialog but keep shop running
                user.setDialog(null);
                removeUser(0);
                // Shop continues to operate as hired merchant
            }
            // PSP opcodes that client sends for EntrustedShop too
            case PSP_DeliverBlackList -> {
                // Client sends blacklist when opening shop - same as ESP_DeliverBlackList
                if (isOpen() || !isOwner(user)) {
                    log.error("Received invalid PSP_DeliverBlackList action");
                    return;
                }
                final int size = inPacket.decodeShort();
                blockedList.clear();
                for (int i = 0; i < size; i++) {
                    blockedList.add(inPacket.decodeString());
                }
            }
            default -> {
                log.error("Unhandled entrusted shop action {}", mrp);
            }
        }
    }

    @Override
    public void leave(User user) {
        final int userIndex = getUserIndex(user);
        if (userIndex == 0) {
            // Owner is leaving the management UI
            // If the shop was already opened before (openTime is set), just exit - don't destroy shop
            // The owner can reopen the shop via MRP_Balloon or close it permanently via MRP_Balloon with open=false
            if (openTime != null) {
                // Shop was opened before - just exit management mode, keep shop in maintenance
                user.write(MiniRoomPacket.leave(0, MiniRoomLeaveType.UserRequest));
                user.setDialog(null);
                removeUser(0);
                // Shop stays in maintenance mode (isOpen=false) - owner can re-enter and click "Open Shop" again
            } else {
                // Shop was never opened (still setting up) - close it completely
                closeShop(MiniRoomLeaveType.UserRequest);
            }
        } else {
            // Guest is leaving
            broadcastPacket(MiniRoomPacket.leave(userIndex, MiniRoomLeaveType.UserRequest));
            removeUser(userIndex);
            user.setDialog(null);
            updateBalloon();
        }
    }

    @Override
    public void updateBalloon() {
        // Only update balloon if shop is open (employee NPC is spawned)
        if (isOpen()) {
            getField().broadcastPacket(FieldPacket.employeeMiniRoomBalloon(this));
        }
    }

    public void closeShop(MiniRoomLeaveType leaveType) {
        // Cancel expiration task if exists
        if (expirationTask != null && !expirationTask.isDone()) {
            expirationTask.cancel(false);
        }

        final User owner = getUser(0);

        if (owner != null) {
            // Owner is online - return items directly to inventory
            final List<InventoryOperation> inventoryOperations = new ArrayList<>();
            for (PlayerShopItem shopItem : items) {
                if (shopItem.bundles == 0) {
                    continue;
                }
                // Restore full quantity: bundles * perBundle
                final int totalQuantity = shopItem.bundles * shopItem.getSetSize();
                final Item returnItem = new Item(shopItem.getItem());
                returnItem.setQuantity((short) totalQuantity);
                final Optional<List<InventoryOperation>> addItemResult = owner.getInventoryManager().addItem(returnItem);
                if (addItemResult.isEmpty()) {
                    // Inventory full - save to Fredrick instead
                    saveItemToFredrick(shopItem);
                } else {
                    inventoryOperations.addAll(addItemResult.get());
                }
            }
            if (!inventoryOperations.isEmpty()) {
                owner.write(WvsContext.inventoryOperation(inventoryOperations, false));
            }

            // Return money to owner
            if (money > 0) {
                if (owner.getInventoryManager().canAddMoney(money)) {
                    owner.getInventoryManager().addMoney(money);
                    owner.write(WvsContext.statChanged(Stat.MONEY, owner.getInventoryManager().getMoney(), false));
                } else {
                    // Owner at meso cap - save to Fredrick instead
                    saveMesoToFredrick(money);
                }
            }
        } else {
            // Owner is offline - save all unsold items to Fredrick
            for (PlayerShopItem shopItem : items) {
                if (shopItem.bundles == 0) {
                    continue;
                }
                saveItemToFredrick(shopItem);
            }
            // Save remaining meso to Fredrick (if any)
            if (money > 0) {
                saveMesoToFredrick(money);
            }
        }

        // Remove guests
        for (int i = 1; i < getMaxUsers(); i++) {
            final User guest = getUser(i);
            if (guest == null) {
                continue;
            }
            guest.write(MiniRoomPacket.leave(i, MiniRoomLeaveType.HostOut));
            guest.setDialog(null);
        }

        // Remove shop
        broadcastPacket(MiniRoomPacket.leave(0, leaveType));
        if (owner != null) {
            owner.setDialog(null);
        }
        getField().getMiniRoomPool().removeMiniRoom(this);
        // Always remove the employee NPC from the field (it may be visible even in maintenance mode)
        getField().broadcastPacket(FieldPacket.employeeLeaveField(employerId));
    }

    /**
     * Save an unsold item to Fredrick (player.shop table)
     * The item's quantity is perBundle, and we save the remaining bundles count.
     * Total quantity when retrieved = bundles * perBundle (item.getQuantity())
     */
    private void saveItemToFredrick(PlayerShopItem shopItem) {
        final ShopItem dbItem = ShopItem.unsold(
                employerId,
                shopItem.getItem(),  // item.quantity = perBundle
                shopItem.getPrice(),
                shopItem.bundles     // remaining bundles
        );
        DatabaseManager.shopAccessor().saveUnsoldItem(dbItem);
    }

    /**
     * Save meso to Fredrick (player.shop table) when owner can't receive it directly.
     * Creates a "sold" record with no item, just meso.
     */
    private void saveMesoToFredrick(int meso) {
        final ShopItem mesoRecord = ShopItem.sold(
                employerId,
                null,   // no item, just meso
                0,      // no price
                (short) 0, // no bundles
                meso,
                null    // no buyer name for meso-only record
        );
        DatabaseManager.shopAccessor().saveSoldItem(mesoRecord);
    }

    private boolean isNoMoreItem() {
        for (PlayerShopItem item : items) {
            if (item.getSetCount() > 0) {
                return false;
            }
        }
        return true;
    }

    public void addVisitor(String characterName) {
        if (!visitList.contains(characterName)) {
            visitList.add(characterName);
        }
    }
}
