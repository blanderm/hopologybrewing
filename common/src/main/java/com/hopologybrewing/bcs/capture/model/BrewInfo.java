package com.hopologybrewing.bcs.capture.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoConstants;

import java.util.Date;

@DynamoDBTable(tableName=DynamoConstants.BREW_INFO_TABLE)
public class BrewInfo {
    private String name;
    private long brewDate;
    private String description;
    private long fermentationComplete = 0L;
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

    @DynamoDBAttribute(attributeName="fermentationComplete")
    public long getFermentationComplete() {
        return fermentationComplete;
    }

    public void setFermentationComplete(long fermentationComplete) {
        this.fermentationComplete = fermentationComplete;
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
        return (fermentationComplete == 0L && now >= brewDate) || (now >= brewDate && now <= fermentationComplete);
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
        return new Date(fermentationComplete);
    }

    public void setFermentationComplete(Date fermentationComplete) {
        this.fermentationComplete = fermentationComplete.getTime();
    }

    @DynamoDBIgnore
    public Date getLastUpdatedAsDate() {
        return new Date(lastUpdated);
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated.getTime();
    }

}
