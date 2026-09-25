package com.hospitality.food.config;

import com.hospitality.food.entity.DietaryType;
import com.hospitality.food.entity.FoodCategory;
import com.hospitality.food.entity.FoodItem;
import com.hospitality.food.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final FoodItemRepository foodItemRepository;

    @Override
    public void run(String... args) {
        if (foodItemRepository.count() > 0) {
            log.info("Food catalog already contains {} items. Skipping initial seeding.", foodItemRepository.count());
            return;
        }

        log.info("Seeding initial culinary menu and food catalog across hotels...");

        List<FoodItem> initialItems = List.of(
                // ==================== HOTEL 1 (The Grand Palace, Mumbai) ====================
                FoodItem.builder()
                        .hotelId(1L)
                        .name("Paneer Tikka Royale")
                        .description("Tandoor-charred cottage cheese cubes marinated in aromatic spices, mustard oil, and yogurt")
                        .category(FoodCategory.APPETIZER)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("350.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(20)
                        .imageUrl("https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Chicken Malai Kebab")
                        .description("Melt-in-mouth chicken tenders infused with cream, cheese, cardamom, and white pepper")
                        .category(FoodCategory.APPETIZER)
                        .dietaryType(DietaryType.NON_VEG)
                        .price(new BigDecimal("480.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(25)
                        .imageUrl("https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Dal Makhani Bukhara")
                        .description("Slow-simmered whole black lentils, tomatoes, churned butter, and double cream cooked for 18 hours")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("380.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(15)
                        .imageUrl("https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Grand Butter Chicken")
                        .description("Boneless tandoori chicken cooked in rich velvety satin tomato and butter gravy with fenugreek")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.NON_VEG)
                        .price(new BigDecimal("520.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(25)
                        .imageUrl("https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Nawabi Dum Biryani")
                        .description("Fragrant basmati rice layered with spiced tender chicken, saffron milk, caramelized onions, and mint")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.NON_VEG)
                        .price(new BigDecimal("550.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(30)
                        .imageUrl("https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Garlic Butter Naan")
                        .description("Clay oven baked fluffy refined flour flatbread brushed with garlic butter and fresh cilantro")
                        .category(FoodCategory.BREADS)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("110.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(10)
                        .imageUrl("https://images.unsplash.com/photo-1601050690597-df0568f70950?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Kesari Gulab Jamun")
                        .description("Warm fried milk solids soaked in rose water, cardamom, and saffron infused sugar syrup")
                        .category(FoodCategory.DESSERT)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("180.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(10)
                        .imageUrl("https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Royal Masala Chai")
                        .description("Handcrafted Assam black tea brewed with crushed ginger, green cardamom, and fresh milk")
                        .category(FoodCategory.BEVERAGE)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("120.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(10)
                        .imageUrl("https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(1L)
                        .name("Classic Club Sandwich")
                        .description("Triple-layer toasted sandwich with crisp lettuce, heirloom tomatoes, cucumber, cheese, and french fries")
                        .category(FoodCategory.SNACKS)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("280.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(15)
                        .imageUrl("https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=500")
                        .build(),

                // ==================== HOTEL 2 (Ocean View Resort, Goa) ====================
                FoodItem.builder()
                        .hotelId(2L)
                        .name("Golden Butter Garlic Prawns")
                        .description("Jumbo Arabian Sea prawns sautéed with crushed garlic, green chillies, butter, and lemon zest")
                        .category(FoodCategory.APPETIZER)
                        .dietaryType(DietaryType.NON_VEG)
                        .price(new BigDecimal("520.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(20)
                        .imageUrl("https://images.unsplash.com/photo-1559742811-82286364ceaf?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(2L)
                        .name("Goan Prawn Curry with Steamed Rice")
                        .description("Succulent prawns cooked in a traditional tangy coconut and kokum sauce served with Goan rice")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.NON_VEG)
                        .price(new BigDecimal("620.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(25)
                        .imageUrl("https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(2L)
                        .name("Coconut Vegetable Xacuti")
                        .description("Assorted garden vegetables braised in roasted coconut, poppy seeds, and Kashmiri red chillies")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.VEGAN)
                        .price(new BigDecimal("380.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(20)
                        .imageUrl("https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(2L)
                        .name("Bebinca Royale")
                        .description("Traditional seven-layered Goan coconut milk and egg yolk pudding scented with nutmeg")
                        .category(FoodCategory.DESSERT)
                        .dietaryType(DietaryType.EGG)
                        .price(new BigDecimal("260.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(10)
                        .imageUrl("https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(2L)
                        .name("Chilled Kokum Cooler")
                        .description("Refreshing coastal drink made with wild kokum extract, roasted cumin, black salt, and mint")
                        .category(FoodCategory.BEVERAGE)
                        .dietaryType(DietaryType.VEGAN)
                        .price(new BigDecimal("160.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(5)
                        .imageUrl("https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500")
                        .build(),

                // ==================== HOTEL 3 (Himalayan Heritage, Shimla) ====================
                FoodItem.builder()
                        .hotelId(3L)
                        .name("Pahadi Paneer Shashlik")
                        .description("Smoked paneer skewers coated with mountain herb paste, dried mint, and mustard seeds")
                        .category(FoodCategory.APPETIZER)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("360.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(20)
                        .imageUrl("https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(3L)
                        .name("Pahadi Rogan Josh")
                        .description("Tender highland lamb cooked slowly with Kashmiri chillies, dried ginger, and aromatic fennel powder")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.NON_VEG)
                        .price(new BigDecimal("640.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(30)
                        .imageUrl("https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(3L)
                        .name("Himachali Chana Madra")
                        .description("White chickpeas slowly simmered in rich spiced yogurt gravy with cloves, cardamom, and ghee")
                        .category(FoodCategory.MAIN_COURSE)
                        .dietaryType(DietaryType.VEG)
                        .price(new BigDecimal("340.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(20)
                        .imageUrl("https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=500")
                        .build(),

                FoodItem.builder()
                        .hotelId(3L)
                        .name("Kashmiri Kahwa Tea")
                        .description("Green tea leaves infused with saffron strands, whole cinnamon, green cardamom, and slivered almonds")
                        .category(FoodCategory.BEVERAGE)
                        .dietaryType(DietaryType.VEGAN)
                        .price(new BigDecimal("140.00"))
                        .isAvailable(true)
                        .preparationTimeMinutes(10)
                        .imageUrl("https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=500")
                        .build()
        );

        foodItemRepository.saveAll(initialItems);
        log.info("Successfully seeded {} gourmet food items across 3 hotels.", initialItems.size());
    }
}
