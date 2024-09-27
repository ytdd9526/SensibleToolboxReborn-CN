package io.github.thebusybiscuit.sensibletoolbox.items;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

public class NetheriteCombineHoe extends CombineHoe {

    public NetheriteCombineHoe() {
        super();
    }

    public NetheriteCombineHoe(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_HOE;
    }

    @Override
    public String getItemName() {
        return "Netherite Combine Hoe";
    }


    public boolean hasGlow() {
        return true;
    }

    @Override
    public Recipe getMainRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getKey(), toItemStack());
        DiamondCombineHoe hoe = new DiamondCombineHoe();
        registerCustomIngredients(hoe);
        recipe.shape("SSS", "HCW", "SSS");
        recipe.setIngredient('S', Material.STRING);
        recipe.setIngredient('H', Material.NETHERITE_HOE);
        recipe.setIngredient('C', hoe.getMaterial());
        recipe.setIngredient('W', Material.NETHERITE_SWORD);
        return recipe;
    }

    @Override
    public int getWorkRadius() {
        return 3;
    }
}
