package dev.otectus.sophisticatedtab.client.compat.legendarytabs;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

public final class SophisticatedBackpacksSizing {
    private static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;
    private static final int SLOT_SIZE = 18;
    private static final int BASE_WIDTH_PADDING = 14;
    private static final int SCROLLBAR_WIDTH_DELTA = 6;
    private static final int VANILLA_INVENTORY_WIDTH = 176;
    private static final int VANILLA_INVENTORY_HEIGHT = 166;

    private SophisticatedBackpacksSizing() {}

    public static int getWidth(Player player) {
        return firstVisibleWrapper(player)
                .map(SophisticatedBackpacksSizing::computeWidth)
                .orElse(VANILLA_INVENTORY_WIDTH);
    }

    public static int getHeight(Player player) {
        return firstVisibleWrapper(player)
                .map(SophisticatedBackpacksSizing::computeHeight)
                .orElse(VANILLA_INVENTORY_HEIGHT);
    }

    private static java.util.Optional<IBackpackWrapper> firstVisibleWrapper(Player player) {
        return BackpackTabResolver.visible(player).stream()
                .findFirst()
                .map(BackpackDescriptor::wrapper);
    }

    private static int computeWidth(IBackpackWrapper wrapper) {
        int totalRows = Math.max(wrapper.getNumberOfSlotRows(), 0);
        int totalSlots = wrapper.getInventoryHandler().getSlots();
        int columnsTaken = Math.max(wrapper.getColumnsTaken(), 0);

        int slotsOnLine = (totalSlots + columnsTaken * totalRows) <= 81 ? 9 : 12;
        int width = slotsOnLine * SLOT_SIZE + BASE_WIDTH_PADDING;

        if (getDisplayableRows(totalRows) < totalRows) {
            width += SCROLLBAR_WIDTH_DELTA;
        }
        return width;
    }

    private static int computeHeight(IBackpackWrapper wrapper) {
        int rows = getDisplayableRows(wrapper.getNumberOfSlotRows());
        return HEIGHT_WITHOUT_STORAGE_SLOTS + rows * SLOT_SIZE;
    }

    private static int getDisplayableRows(int totalRows) {
        int safeTotal = Math.max(totalRows, 0);
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return safeTotal;
        }
        int viewportRows = Math.max((mc.getWindow().getGuiScaledHeight() - HEIGHT_WITHOUT_STORAGE_SLOTS) / SLOT_SIZE, 0);
        return Math.min(viewportRows, safeTotal);
    }
}
