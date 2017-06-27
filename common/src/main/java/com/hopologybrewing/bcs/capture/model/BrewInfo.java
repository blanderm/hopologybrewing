package com.hopologybrewing.bcs.capture.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoConstants;

import java.util.Date;
import java.util.List;

@DynamoDBTable(tableName=DynamoConstants.BREW_INFO_TABLE)
public class BrewInfo {
    private String name;
    private long brewDate;
    private String description;
    private long crashStart = 0L;
    private long brewCompleteDate = 0L;
    private long lastUpdated;

    @DynamoDBAttribute(attributeName="name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBHashKey(attributeName="brewDate")
    public long getBrewDate() {
        return brewDate;
    }

    public void setBrewDate(long brewDate) {
        this.brewDate = brewDate;
    }

    @DynamoDBAttribute(attributeName="description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @DynamoDBAttribute(attributeName="brewCompleteDate")
    public long getBrewCompleteDate() {
        return brewCompleteDate;
    }

    public void setBrewCompleteDate(long brewCompleteDate) {
        this.brewCompleteDate = brewCompleteDate;
    }


    @DynamoDBAttribute(attributeName="lastUpdated")
    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @DynamoDBIgnore
    public boolean isCurrentBrew() {
        long now = new Date().getTime();
        return (brewCompleteDate == 0L && now >= brewDate) || (now >= brewDate && now <= brewCompleteDate);
    }

    @DynamoDBIgnore
    public Date getBrewDateAsDate() {
        return new Date(brewDate);
    }

    public void setBrewDate(Date brewDate) {
        this.brewDate = brewDate.getTime();
    }

    @DynamoDBIgnore
    public Date getFermentationCompleteAsDate() {
        return new Date(brewCompleteDate);
    }

    public void setFermentationComplete(Date fermentationComplete) {
        this.brewCompleteDate = fermentationComplete.getTime();
    }

    @DynamoDBIgnore
    public Date getLastUpdatedAsDate() {
        return new Date(lastUpdated);
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated.getTime();
    }

    @DynamoDBAttribute(attributeName="crashStart")
    public long getCrashStart() {
        return crashStart;
    }

    public void setCrashStart(long crashStart) {
        this.crashStart = crashStart;
    }

    @DynamoDBIgnore
    public Date getCrashStartAsDate() {
        return new Date(crashStart);
    }

    /*
            Returns -1 if m,ost recent can't be found
         */
    public static int getMostRecentBrewIndex(List<BrewInfo> list) {
        int mostRecentIndex = -1;
        long mostRecentBrewDate = 0L;

        BrewInfo current = null;
        for (int i = 0; i < list.size(); i++) {
            current = list.get(i);

            if (current.isCurrentBrew()) {
                mostRecentIndex = i;
                mostRecentBrewDate = current.getBrewDate();
                break;
            } else if (mostRecentIndex == -1 || current.getBrewDate() > mostRecentBrewDate) {
                mostRecentIndex = i;
                mostRecentBrewDate = current.getBrewDate();
            }
        }

        return mostRecentIndex;
    }
}
