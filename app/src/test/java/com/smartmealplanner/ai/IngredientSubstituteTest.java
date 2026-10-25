package com.smartmealplanner.ai;

import com.smartmealplanner.ai.model.IngredientSubstitute;
import com.smartmealplanner.ai.model.SubstituteOption;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class IngredientSubstituteTest {

    @Test
    public void testIngredientSubstitute_ExactAndAliasMatching() {
        SubstituteOption opt = new SubstituteOption(
                "Firm Tofu", "1:1 by weight", "VEGAN",
                "Curry protein", "Holds shape and absorbs sauce",
                "Serious Eats", "https://www.seriouseats.com/tofu-guide"
        );
        IngredientSubstitute sub = new IngredientSubstitute(
                "Fresh Paneer",
                "Dairy / Protein",
                Arrays.asList("paneer", "cottage cheese", "fresh paneer", "chenna"),
                Collections.singletonList(opt)
        );

        // Exact match
        assertTrue("Should match canonical name", sub.matches("Fresh Paneer"));
        assertTrue("Should match lowercase canonical name", sub.matches("fresh paneer"));

        // Alias matches
        assertTrue("Should match alias 'paneer'", sub.matches("paneer"));
        assertTrue("Should match alias 'cottage cheese'", sub.matches("cottage cheese"));
        assertTrue("Should match alias 'chenna'", sub.matches("chenna"));

        // Match with punctuation or surrounding text
        assertTrue("Should match 'Paneer (cubed)'", sub.matches("Paneer (cubed)"));

        // Non-matches
        assertFalse("Should not match unrelated string", sub.matches("Almond Milk"));
        assertFalse("Should handle null query gracefully", sub.matches(null));
        assertFalse("Should handle empty query gracefully", sub.matches("   "));
    }

    @Test
    public void testSubstituteOption_PropertiesAndGetters() {
        SubstituteOption opt = new SubstituteOption(
                "Yellow Split Peas",
                "1:1 by weight",
                "VEGAN, GLUTEN-FREE",
                "Tadka dal & sambar base",
                "Similar starch and cooking breakdown to toor dal",
                "The Spruce Eats",
                "https://www.thespruceeats.com/yellow-split-peas-substitutes"
        );

        assertEquals("Yellow Split Peas", opt.getName());
        assertEquals("1:1 by weight", opt.getRatio());
        assertEquals("VEGAN, GLUTEN-FREE", opt.getDietaryTag());
        assertEquals("Tadka dal & sambar base", opt.getCulinaryRole());
        assertEquals("Similar starch and cooking breakdown to toor dal", opt.getReason());
        assertEquals("The Spruce Eats", opt.getSourceName());
        assertTrue(opt.getSourceUrl().startsWith("https://"));
    }

    @Test
    public void testIngredientSubstitutesJson_AssetIntegrity() throws Exception {
        // Locate asset file from module root or current working directory
        File assetFile = new File("src/main/assets/ingredient_substitutes.json");
        if (!assetFile.exists()) {
            assetFile = new File("app/src/main/assets/ingredient_substitutes.json");
        }
        assertTrue("ingredient_substitutes.json asset must exist", assetFile.exists());

        String jsonContent = new String(Files.readAllBytes(assetFile.toPath()), StandardCharsets.UTF_8);
        assertNotNull("JSON content must not be null", jsonContent);
        assertTrue("Asset file must not be empty", jsonContent.length() > 500);

        // Verify key staples are defined in the knowledge base
        assertTrue("Must include Paneer", jsonContent.contains("\"Fresh Paneer\""));
        assertTrue("Must include Whole Wheat Atta", jsonContent.contains("\"Whole Wheat Atta\""));
        assertTrue("Must include Toor Dal", jsonContent.contains("\"Toor Dal (Lentils)\""));
        assertTrue("Must include Ghee / Cooking Oil", jsonContent.contains("\"Ghee / Cooking Oil\""));
        assertTrue("Must include Chickpeas", jsonContent.contains("\"Chickpeas (Kabuli Chana)\""));
        assertTrue("Must include Whole Eggs", jsonContent.contains("\"Whole Eggs\""));
        assertTrue("Must include Milk Curd / Yogurt", jsonContent.contains("\"Pasteurized Milk Curd\""));
        assertTrue("Must include Basmati Rice", jsonContent.contains("\"Basmati Rice\""));
        assertTrue("Must include Chicken", jsonContent.contains("\"Chicken Breast / Thighs\""));
        assertTrue("Must include Mozzarella", jsonContent.contains("\"Mozzarella Cheese\""));

        // Verify credible authority source domains are cited
        assertTrue("Must cite Serious Eats", jsonContent.contains("Serious Eats"));
        assertTrue("Must cite The Spruce Eats", jsonContent.contains("The Spruce Eats"));
        assertTrue("Must cite Healthline", jsonContent.contains("Healthline Nutrition"));
        assertTrue("Must cite USDA", jsonContent.contains("USDA FoodData Central"));
        assertTrue("Must cite BBC Good Food", jsonContent.contains("BBC Good Food"));
        assertTrue("All URLs must be HTTPS", jsonContent.contains("\"sourceUrl\": \"https://"));
    }
}
