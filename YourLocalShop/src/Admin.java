import java.util.Scanner;

public class Admin {

    private final ProductCatalog catalog;
    private final InventoryManager inventory;
    private final Scanner scanner = new Scanner(System.in);

    public Admin(ProductCatalog catalog, InventoryManager inventory) {
        this.catalog = catalog;
        this.inventory = inventory;
    }

    public void start() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== Admin ===");
            System.out.println("1. View items");
            System.out.println("2. Add item");
            System.out.println("3. Delete item");
            System.out.println("4. Edit item");
            System.out.println("0. Back");
            System.out.print("Select: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "1" -> catalog.displayAll();
                case "2" -> adminAddItem();
                case "3" -> adminDeleteItem();
                case "4" -> adminEditItem();
                case "0" -> back = true;
                default -> System.out.println("Invalid.");
            }
        }
    }

    private void adminAddItem() {
    }

    private void adminDeleteItem() {
    }

    private void adminEditItem() {
    }
}
