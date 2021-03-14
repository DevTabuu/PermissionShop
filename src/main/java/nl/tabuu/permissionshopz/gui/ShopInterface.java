package nl.tabuu.permissionshopz.gui;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import nl.tabuu.permissionshopz.PermissionShopZ;
import nl.tabuu.permissionshopz.data.Perk;
import nl.tabuu.permissionshopz.permissionhandler.IPermissionHandler;
import nl.tabuu.permissionshopz.util.Message;
import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.economy.hook.Vault;
import nl.tabuu.tabuucore.inventory.InventorySize;
import nl.tabuu.tabuucore.inventory.ui.InventoryFormUI;
import nl.tabuu.tabuucore.inventory.ui.element.Button;
import nl.tabuu.tabuucore.inventory.ui.element.style.Style;
import nl.tabuu.tabuucore.inventory.ui.graphics.brush.CheckerBrush;
import nl.tabuu.tabuucore.inventory.ui.graphics.brush.IBrush;
import nl.tabuu.tabuucore.item.ItemBuilder;
import nl.tabuu.tabuucore.material.XMaterial;
import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tabuucore.util.vector.Vector2f;
import org.bukkit.entity.Player;

import java.util.*;

public class ShopInterface extends InventoryFormUI {
    protected final Economy _economy;
    protected final Dictionary _local;
    protected final IConfiguration _config;
    protected final IPermissionHandler _permission;

    private int _page;
    private final int _maxPage;
    private final Player _player;
    private final List<Perk> _perks;

    public ShopInterface(Player player) {
        super("", InventorySize.THREE_ROWS);

        _economy = Vault.getEconomy();
        _config = PermissionShopZ.getInstance().getConfiguration();
        _local = PermissionShopZ.getInstance().getLocal();
        _permission = PermissionShopZ.getInstance().getPermissionHandler();

        InventorySize size = _config.get("GUISize", InventorySize::valueOf);
        if(size != null && size.getHeight() >= 3) setSize(size);

        _player = player;
        _perks = new ArrayList<>(PermissionShopZ.getInstance().getPerkManager().getPerks());

        int contentWidth = getSize().getWidth() - 2;
        int contentHeight = getSize().getHeight() - 2;
        _maxPage = _perks.size() / (contentWidth * contentHeight);

        setTitle(_local.translate("GUI_SHOP_TITLE", getReplacements()));
    }

    @Override
    protected void onDraw() {
        ItemBuilder
                next = new ItemBuilder(XMaterial.GREEN_STAINED_GLASS_PANE)
                        .setDisplayName(_local.translate("GUI_PAGE_NEXT", getReplacements())),

                previous = new ItemBuilder(XMaterial.GREEN_STAINED_GLASS_PANE)
                        .setDisplayName(_local.translate("GUI_PAGE_PREVIOUS", getReplacements())),

                barrier = new ItemBuilder(XMaterial.BARRIER)
                        .setDisplayName(_local.translate("GUI_PAGE_CLOSE", getReplacements())),

                black = new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE)
                        .setDisplayName(" "),

                gray = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE)
                        .setDisplayName(" ");

        Style
                nextButtonStyle = new Style(next.build(), next.build()),
                previousButtonStyle = new Style(previous.build(), previous.build()),
                exitButtonStyle = new Style(barrier.build(), barrier.build()),
                clearButtonStyle = new Style(XMaterial.AIR, XMaterial.AIR);

        Button
                nextButton = new Button(nextButtonStyle, this::nextPage),
                previousButton = new Button(previousButtonStyle, this::previousPage),
                exitButton = new Button(exitButtonStyle, this::close),
                clearButton = new Button(clearButtonStyle);

        IBrush brush = new CheckerBrush(black.build(), gray.build());
        setBrush(brush);

        Vector2f borderStart = new Vector2f(0, 0);
        Vector2f borderStop = new Vector2f(getSize().getWidth() - 1, getSize().getHeight() - 1);
        drawRectangle(borderStart, borderStop);

        setElement(new Vector2f(borderStop.getX(), borderStop.getY()), nextButton);
        setElement(new Vector2f(borderStart.getX(), borderStop.getY()), previousButton);
        setElement(new Vector2f(borderStop.getX() / 2, borderStop.getY()), exitButton);

        int rowSize = getSize().getWidth() - 2;
        int maxRows = getSize().getHeight() - 2;
        Vector2f offset = new Vector2f(1, 1);

        for(int i = 0; i < rowSize * maxRows; i++) {
            int index = getPage() * (rowSize * maxRows) + i;
            int x = i % rowSize;
            int y = i / rowSize;

            Vector2f position = new Vector2f(x, y).add(offset);

            if(index < _perks.size()) {
                Perk perk = _perks.get(index);
                Button button = createPerkItem(_player, perk);
                setElement(position, button);
            }
            else setElement(position, clearButton);
        }
        super.onDraw();
    }

    protected Button createPerkItem(Player player, Perk perk) {
        XMaterial unlockedMaterial = _config.get("UnlockedMaterial", XMaterial::valueOf);
        assert unlockedMaterial != null : "UnlockedMaterial has not been correctly set in the config.";

        boolean unlocked = perk.getAwardedPermissions().stream().allMatch(node -> _permission.hasPermission(_player, node));

        ItemBuilder displayItemBuilder = new ItemBuilder(perk.getDisplayItem());
        ItemBuilder unlockedDisplayItemBuilder = new ItemBuilder(unlockedMaterial);

        String displayName = _local.translate("GUI_PERK_NAME", perk.getReplacements());
        displayItemBuilder.setDisplayName(displayName);
        unlockedDisplayItemBuilder.setDisplayName(displayName);

        if (_config.getBoolean("DisplayPermissionList", true)) {
            String header = _local.translate("GUI_PERK_AWARDED_PERMISSION_HEADER");
            displayItemBuilder.addLore(header);
            unlockedDisplayItemBuilder.addLore(header);

            for (String node : perk.getAwardedPermissions()) {
                String line = _permission.hasPermission(_player, node) ? "GUI_PERK_AWARDED_PERMISSION_ENTRY_HAS" : "GUI_PERK_AWARDED_PERMISSION_ENTRY";
                line = _local.translate(line, "{PERMISSION}", node);
                displayItemBuilder.addLore(line);
                unlockedDisplayItemBuilder.addLore(line);
            }
        }

        if (_config.getBoolean("DisplayRequiredPermissionList", true)) {
            String header = _local.translate("GUI_PERK_REQUIRED_PERMISSION_HEADER");
            displayItemBuilder.addLore(header);
            unlockedDisplayItemBuilder.addLore(header);

            for (String node : perk.getRequiredPermissions()) {
                String entry = _permission.hasPermission(_player, node) ? "GUI_PERK_REQUIRED_PERMISSION_ENTRY_HAS" : "GUI_PERK_REQUIRED_PERMISSION_ENTRY";
                entry = _local.translate(entry, "{PERMISSION}", node);
                displayItemBuilder.addLore(entry);
                unlockedDisplayItemBuilder.addLore(entry);
            }
        }

        displayItemBuilder.addLore(_local.translate("GUI_PERK_LOCKED_FOOTER", perk.getReplacements()));
        unlockedDisplayItemBuilder.addLore(_local.translate("GUI_PERK_UNLOCKED_FOOTER", perk.getReplacements()));

        Style style = new Style(displayItemBuilder.build(), unlockedDisplayItemBuilder.build());
        Button button = new Button(style, p -> onPerkClick(player, perk));
        button.setEnabled(!unlocked);

        return button;
    }

    protected void onPerkClick(Player player, Perk perk) {
        EconomyResponse response = _economy.withdrawPlayer(player, perk.getCost());

        if(!perk.hasRequiredPermissions(player))
            Message.send(player, _local.translate("ERROR_INSUFFICIENT_PERMISSION", perk.getReplacements()));

        else if(response.type.equals(EconomyResponse.ResponseType.SUCCESS)) {
            perk.apply(player);
            Message.send(player, _local.translate("PERK_BUY_SUCCESS", perk.getReplacements()));
        } else
            Message.send(player, _local.translate("ERROR_INSUFFICIENT_FUNDS", perk.getReplacements()));

        updatePage();
    }

    protected void updatePage() {
        String raw = _local.getOrDefault("GUI_SHOP_TITLE", "GUI_SHOP_TITLE");
        if(Objects.nonNull(raw) && raw.contains("{PAGE}")) {
            setTitle(_local.translate("GUI_SHOP_TITLE", getReplacements()));
            reload();
        }
        onDraw();
    }

    protected void nextPage(Player player) {
        if (getPage() < getMaxPage()) {
            _page++;
            updatePage();
        }
    }

    protected void previousPage(Player player) {
        if (getPage() > 0) {
            _page--;
            updatePage();
        }
    }

    protected Object[] getReplacements() {
        return new Object[] {
                "{PAGE}", getPage() + 1,
                "{MAX_PAGE}", _maxPage + 1,
                "{PLAYER}", _player.getName()
        };
    }

    public int getPage() {
        return _page;
    }

    public int getMaxPage() {
        return _maxPage;
    }
}