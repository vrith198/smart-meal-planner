package com.smartmealplanner.ai.model;

import java.io.Serializable;

public class SubstituteOption implements Serializable {
    private String name;
    private String ratio;
    private String dietaryTag;
    private String culinaryRole;
    private String reason;
    private String sourceName;
    private String sourceUrl;

    public SubstituteOption() {}

    public SubstituteOption(String name, String ratio, String dietaryTag,
                            String culinaryRole, String reason,
                            String sourceName, String sourceUrl) {
        this.name = name;
        this.ratio = ratio;
        this.dietaryTag = dietaryTag;
        this.culinaryRole = culinaryRole;
        this.reason = reason;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRatio() {
        return ratio;
    }

    public void setRatio(String ratio) {
        this.ratio = ratio;
    }

    public String getDietaryTag() {
        return dietaryTag;
    }

    public void setDietaryTag(String dietaryTag) {
        this.dietaryTag = dietaryTag;
    }

    public String getCulinaryRole() {
        return culinaryRole;
    }

    public void setCulinaryRole(String culinaryRole) {
        this.culinaryRole = culinaryRole;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
