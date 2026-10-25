package com.smartmealplanner.ai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IngredientSubstitute implements Serializable {
    private String canonicalName;
    private String category;
    private List<String> aliases = new ArrayList<>();
    private List<SubstituteOption> substitutes = new ArrayList<>();

    public IngredientSubstitute() {}

    public IngredientSubstitute(String canonicalName, String category,
                                List<String> aliases, List<SubstituteOption> substitutes) {
        this.canonicalName = canonicalName;
        this.category = category;
        this.aliases = aliases != null ? aliases : new ArrayList<>();
        this.substitutes = substitutes != null ? substitutes : new ArrayList<>();
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public List<SubstituteOption> getSubstitutes() {
        return substitutes;
    }

    public void setSubstitutes(List<SubstituteOption> substitutes) {
        this.substitutes = substitutes;
    }

    /**
     * Checks if this ingredient matches the provided query string.
     * Performs case-insensitive matching across canonical name and aliases.
     */
    public boolean matches(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        String cleanQuery = clean(query);
        if (clean(canonicalName).contains(cleanQuery) || cleanQuery.contains(clean(canonicalName))) {
            return true;
        }

        if (aliases != null) {
            for (String alias : aliases) {
                String cleanAlias = clean(alias);
                if (cleanAlias.equals(cleanQuery) || cleanQuery.contains(cleanAlias) || cleanAlias.contains(cleanQuery)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String clean(String input) {
        if (input == null) return "";
        return input.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
