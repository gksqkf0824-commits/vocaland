package vocaland;

import vocaland.ui.util.ImagePaths;

import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import vocaland.service.LocalData;
import vocaland.ui.lobby.LobbyPanel;
import vocaland.ui.login.LoginPanel;
import vocaland.ui.util.ImageAssets;
import vocaland.ui.scoreboard.ScoreboardPanel;
import vocaland.ui.store.StorePanel;

/** Run with java -Djava.awt.headless=true -cp <build> vocaland.LocalDataIntegrationTest. */
public final class LocalDataIntegrationTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static boolean hasText(Container parent, String text) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel label && text.equals(label.getText())) return true;
            if (c instanceof Container child && hasText(child, text)) return true;
        }
        return false;
    }
    private static JButton button(Container parent, String name) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JButton b && name.equals(b.getAccessibleContext().getAccessibleName())) return b;
            if (c instanceof Container child) { JButton result = button(child, name); if (result != null) return result; }
        }
        return null;
    }
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container nested) layout(nested);
    }
    private static void checkStoreLayout(StorePanel store) {
        store.setSize(1000, 720);
        layout(store);
        JPanel split = (JPanel) ((BorderLayout) store.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        Rectangle left = split.getComponent(0).getBounds(), right = split.getComponent(1).getBounds();
        for (String category : new String[]{"농구", "배경", "캐릭터", "농구", "캐릭터"}) {
            button(store, category).doClick(); layout(store);
            try {
                var field = StorePanel.class.getDeclaredField("gridPanel"); field.setAccessible(true);
                JPanel grid = (JPanel) field.get(store);
                check(grid.getComponentCount() == (category.equals("캐릭터") ? 8 : 6), "Category slot count");
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            check(left.equals(split.getComponent(0).getBounds()) && right.equals(split.getComponent(1).getBounds()), "Store category must not resize columns");
        }
        try {
            var field = StorePanel.class.getDeclaredField("gridPanel"); field.setAccessible(true);
            JPanel grid = (JPanel) field.get(store);
            check(grid.getComponentCount() == 8, "Store must display eight character cards");
            check(Arrays.stream(grid.getComponents()).allMatch(c -> c.getMouseListeners().length > 0), "All eight slots must contain selectable skins");
            for (Component card : grid.getComponents()) {
                if (card.getMouseListeners().length == 0) continue;
                card.dispatchEvent(new java.awt.event.MouseEvent(card, java.awt.event.MouseEvent.MOUSE_CLICKED,
                        System.currentTimeMillis(), 0, 10, 10, 1, false));
                layout(store);
                check(left.equals(split.getComponent(0).getBounds()) && right.equals(split.getComponent(1).getBounds()), "Store selection must not resize columns");
            }
            ImageIcon icon = ImageAssets.load(ImagePaths.ROOT + ImagePaths.STORE_CHARACTER_GIRYONG_BASIC, 85, 85, 64, true);
            check(icon.getIconHeight() == 85 && icon.getIconWidth() >= 60, "Basic character must fill its portrait frame");
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("vocaland-data-test-");
        try {
            Files.copy(Path.of("data/words.txt"), directory.resolve("words.txt"));
            Files.copy(Path.of("data/items.txt"), directory.resolve("items.txt"));
            LocalData data = new LocalData(directory);
            check(data.current() == null, "First launch must show login");
            for (String bad : new String[]{" ", "A|B", "A\nB"}) {
                try { data.login(bad); throw new AssertionError("Invalid name accepted"); } catch (IllegalArgumentException expected) {}
            }
            String alice = data.login(" Alice ").id();
            check(data.current().name().equals("Alice") && data.current().coins() == 0, "New profile");
            var characters = data.items().stream().filter(i -> i.type().equals("CHARACTER")).toList();
            check(characters.size() == 8 && characters.get(0).id().equals("C03"), "Eight skins, basic first");
            check(data.equipped("C03") && characters.stream().filter(i -> !i.id().equals("C03"))
                    .allMatch(i -> i.price() > 0 && !data.owns(i.id())), "Only basic skin granted to new profiles");
            for (var item : characters) check(new ImageIcon(item.image()).getIconWidth() > 0, "Skin image must load: " + item.image());
            check(data.owns("B01") && data.equipped("B01"), "Default inventory");
            check(new LocalData(directory).current().id().equals(alice), "Auto login after restart");
            data.finishGame("RAIN", "NORMAL", 1000);
            data.finishGame("RAIN", "NORMAL", 500);
            data.finishGame("BASKETBALL", "HARD", 5000);
            check(data.current().coins() == 650 && data.current().xp() == 6500, "Rewards");
            check(data.ranks("RAIN").size() == 1 && data.ranks("RAIN").get(0).score() == 1000, "Highest score, no duplicate ranks");
            data.buyOrEquip("B02");
            check(data.current().coins() == 250 && data.equipped("B02") && !data.equipped("B01"), "Purchase and equip");
            data.buyOrEquip("B02"); check(data.current().coins() == 250, "Owned items must not charge again");
            var inventory = Files.readAllLines(directory.resolve("inventory.txt"));
            try { data.buyOrEquip("B03"); throw new AssertionError("Insufficient funds accepted"); } catch (IllegalArgumentException expected) {}
            check(Files.readAllLines(directory.resolve("inventory.txt")).equals(inventory) && data.current().coins() == 250, "Failed purchase must not mutate");
            data.recordAnswer("apple", true); data.recordAnswer("apple", false);
            check(Files.readAllLines(directory.resolve("records.txt")).size() == 2, "Answer history");
            data.logout(); check(new LocalData(directory).current() == null, "Logout persists");
            String bob = data.login("Bob").id();
            check(!bob.equals(alice) && data.current().coins() == 0 && !data.owns("B02"), "Profile isolation");
            data.finishGame("RAIN", "NORMAL", 1000);
            data.login("Carol"); data.finishGame("RAIN", "NORMAL", 100);
            var ranks = data.ranks("RAIN");
            check(ranks.get(0).position() == 1 && ranks.get(1).position() == 1 && ranks.get(2).position() == 3, "Tied ranks");
            data.logout(); data.login("Alice");
            check(data.current().id().equals(alice) && data.current().coins() == 250 && data.equipped("B02"), "Existing name restores profile");
            check(data.users().size() == 3, "No duplicate profiles");
            LocalData reopened = new LocalData(directory);
            check(reopened.equippedImage("BALL").equals(ImagePaths.ROOT + ImagePaths.STORE_BASKETBALL_BALL_NEON), "Equipment persists");
            check(reopened.ranks("BASKETBALL").get(0).score() == 5000, "Scores persist");
            SwingUtilities.invokeAndWait(() -> {
                ScoreboardPanel scoreboard = new ScoreboardPanel(reopened, () -> {});
                check(hasText(scoreboard, "Alice") && hasText(scoreboard, "1,000점"), "Ranking reads persisted data");
                StorePanel store = new StorePanel(reopened, () -> {}); store.refreshData();
                checkStoreLayout(store);
                final int[] opened = {0};
                LobbyPanel main = new LobbyPanel(reopened, () -> {}, () -> {}, () -> {}, () -> {}, () -> {}, () -> opened[0]++);
                JButton rank = button(main, "랭킹"); check(rank != null, "Ranking button"); rank.doClick();
                check(opened[0] == 1, "Ranking callback once");
                check(button(main, "Alice") != null && button(main, "코인 250, 상점 열기") != null, "Main profile and wallet");
                new LoginPanel(name -> {});
            });
            reopened.login("Skin purchase test");
            reopened.finishGame("RAIN", "NORMAL", 100000);
            int wallet = reopened.current().coins();
            for (var skin : characters) {
                reopened.buyOrEquip(skin.id());
                wallet -= skin.price();
                check(reopened.current().coins() == wallet && reopened.equipped(skin.id()), "Skin purchase and equip: " + skin.id());
                check(characters.stream().filter(i -> reopened.equipped(i.id())).count() == 1, "One character equipped at a time");
            }
            reopened.buyOrEquip("H01");
            reopened.buyOrEquip("B01");
            check(reopened.equipped("H01") && reopened.equipped("B01"), "Ball and hoop equip independently");
            reopened.login("Alice");
            // Unreadable inventory must leave the wallet untouched.
            Files.delete(directory.resolve("inventory.txt")); Files.createDirectory(directory.resolve("inventory.txt"));
            try { reopened.buyOrEquip("B01"); throw new AssertionError("Broken inventory accepted"); } catch (RuntimeException expected) {}
            check(reopened.current().coins() == 250, "Read failure must preserve wallet");
            System.out.println("PASS: login/session, isolation, rewards, rankings/ties, inventory, persistence, UI data binding");
        } finally {
            try (var files = Files.walk(directory)) { for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
        }
    }
}
